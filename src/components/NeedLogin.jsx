import { Link } from 'react-router-dom';
import Modal from './Modal';
import { URL } from '/config/constants';

function NeedLogin(props) {
    return (
        <Modal {...props}>
            <div className="d-flex flex-column align-items-stretch text-center gap-8 pb-4" style={{ width: 300 }}>
                <div className="m-3">
                    <img src="/assets/icons/ico_lock.svg" alt="" />
                </div>
                <p className="h5 m-0">로그인이 필요한 서비스 입니다.</p>
                <p className="small mb-3">로그인하시고 더 많은 서비스를 이용해 보세요.</p>
                <Link to={`${URL.LOGIN}`} element="button" className="border-radius-12 p-2 bg-point text-white bold">로그인</Link>
            </div>
        </Modal>
    );
}

export default NeedLogin;
