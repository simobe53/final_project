# fraud_model.py
import re
from web3 import Web3
from web3.middleware.proof_of_authority import ExtraDataToPOAMiddleware
import os
import json
from dotenv import load_dotenv
import random

load_dotenv()

class FraudDetectionModel:
    def __init__(self):
        self.RPC_URL = os.getenv("RPC_URL")
        self.PRIVATE_KEY = os.getenv("PRIVATE_KEY")
        self.ACCOUNT = os.getenv("ACCOUNT")
        self.CONTRACT_ADDRESS = os.getenv("CONTRACT_ADDRESS")  # 배포 후 주소

        self.w3 = Web3(Web3.HTTPProvider(self.RPC_URL))
        self.w3.middleware_onion.inject(ExtraDataToPOAMiddleware, layer=0)
        # 컨트랙트 ABI
        self.ABI = [
            {
                "inputs": [{"internalType": "bytes32", "name": "detectionHash", "type": "bytes32"}],
                "name": "recordDetection",
                "outputs": [],
                "stateMutability": "nonpayable",
                "type": "function"
            }
        ]
        self.contract = None
        if self.CONTRACT_ADDRESS:
            if Web3.is_address(self.CONTRACT_ADDRESS):
                self.contract = self.w3.eth.contract(address=self.CONTRACT_ADDRESS, abi=self.ABI)
            else:
                print(f"⚠️ 잘못된 CONTRACT_ADDRESS: {self.CONTRACT_ADDRESS}")
                
    def deploy_contract(self, bytecode: str) -> str:
        """
        컨트랙트 배포 후 주소 반환
        """
        contract = self.w3.eth.contract(abi=self.ABI, bytecode=bytecode)
        nonce = self.w3.eth.get_transaction_count(self.ACCOUNT)
        txn = contract.constructor().build_transaction({
            "from": self.ACCOUNT,
            "nonce": nonce,
            "gas": 3000000,
            "gasPrice": self.w3.to_wei("10", "gwei"),
        })
        signed = self.w3.eth.account.sign_transaction(txn, self.PRIVATE_KEY)
        tx_hash = self.w3.eth.send_raw_transaction(signed.rawTransaction)
        tx_receipt = self.w3.eth.wait_for_transaction_receipt(tx_hash)

        self.CONTRACT_ADDRESS = tx_receipt.contractAddress
        self.contract = self.w3.eth.contract(address=self.CONTRACT_ADDRESS, abi=self.ABI)
        return self.CONTRACT_ADDRESS

    def record_detection(self, raw_data) -> dict:
        if isinstance(raw_data, dict):
            payment_dict = raw_data
        else:
            raw_str = raw_data.strip()
            raw_str = re.sub(r'([{,]\s*)(\w+)\s*:', r'\1"\2":', raw_str)
            raw_str = re.sub(r':\s*([A-Za-z_][A-Za-z0-9_]*)', r':"\1"', raw_str)
            raw_str = re.sub(r'"paymentId":([0-9a-f]+)', r'"paymentId":"\1"', raw_str, flags=re.IGNORECASE)
            try:
                payment_dict = json.loads(raw_str)
            except Exception as e:
                print("❌ JSON 변환 실패:", e)
                print("raw_str =", raw_str)
                return {"error": "JSON 파싱 실패", "raw": raw_str}

        print("✅ 변환된 데이터:", payment_dict)

        score = random.uniform(0, 1)
        is_suspicious = score > 0.8

        try:
            latest_block = self.w3.eth.get_block('latest')
            timestamp = latest_block['timestamp']
        except Exception as e:
            print("❌ 블록 정보 조회 실패:", e)
            return {"error": "블록 정보 조회 실패", "exception": str(e)}

        detection_data = f"{payment_dict}|score:{score:.2f}|result:{'fraudulent' if is_suspicious else 'normal'}|timestamp:{timestamp}"

        if not self.contract:
            return {"error": "⚠️ 컨트랙트 미배포 또는 주소 오류"}

        hash_bytes = self.w3.keccak(text=detection_data)
        nonce = self.w3.eth.get_transaction_count(self.ACCOUNT)

        txn = self.contract.functions.recordDetection(hash_bytes).build_transaction({
            "from": self.ACCOUNT,
            "nonce": nonce,
            "gas": 200000,
            "gasPrice": self.w3.to_wei("10", "gwei"),
        })
        signed = self.w3.eth.account.sign_transaction(txn, private_key=self.PRIVATE_KEY)
        tx_hash = self.w3.eth.send_raw_transaction(signed.rawTransaction)

        print(f"✅ 블록체인 기록 완료: {tx_hash.hex()}")

        return {
            "success": True,
            "tx_hash": tx_hash.hex(),
            "hash": hash_bytes.hex(),
            "score": score,
            "is_suspicious": is_suspicious,
            "parsed": payment_dict
        }