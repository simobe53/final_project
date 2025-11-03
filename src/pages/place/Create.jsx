import { useEffect, useRef, useState } from "react"
import { useNavigate, useOutletContext } from "react-router-dom";
import { OverlayPage } from "/components";
import { getMapfromAddress, setMarkerfromAddress } from "/components/Map";
import { useAuth } from "/context/AuthContext";
import { useInit } from "/context/InitContext";
import { fileToBase64 } from "/components/File";
import { createPlace } from "/services/places";
import { URL } from "/config/constants";
import NotAuthorized from "/pages/NotAuthorized";

const categories = [
    "카페", 
    "양식",
    "치킨",
    "피자",
    "중식",
    "한식",
    "분식",
    "주점/바",
    "술집",
    "기타"
];

export default function CreatePlace() {
    const { setPlaces } = useOutletContext();
    const { auth } = useAuth();
    const { teams } = useInit();
    const [form, setForm] = useState({
        name: '',
        address: '',
        userId: auth.id,
        category: '카페',
        team: 'LG'
    });
    const { address, category } = form;
    const [file, setFile] = useState({});
    const navigate = useNavigate();
    const nameRef = useRef();
    const addressRef = useRef();
    const mapRef = useRef();
    //<<유효성 체크 메시지 출력을 위한 State>>
    const [nameValid, setNameValid] = useState('');
    const [addressValid, setAddressValid] = useState('');
    //<<게시글 등록 버튼 이벤트 처리용>>
    const handleSubmit = e=>{
        e.preventDefault();//제출 기능 막기
        const nameNode = nameRef.current;
        const addressNode = addressRef.current;

        if(nameNode.value.trim()===''){
            setNameValid('제목을 입력하세요');
            nameNode.focus();
        }
        if(addressNode.value.trim()===''){
            setAddressValid('내용을 입력하세요');
            addressNode.focus();
        }
        if(nameNode.value.trim()==='' || addressNode.value.trim()==='') return;

        const [tteam] = teams.filter(({ idKey }) => idKey === form.team);
        
        createPlace({
            name: nameRef.current.value,
            address,
            image: file.file,
            category,
            user: { id: auth.id, account: auth.account },   
            team: tteam
        })
        .then(data => {
            if (data.id) {
                alert("플레이스가 생성되었습니다");
                navigate(URL.PLACE, { replace: true });
                // 성공시 지도에 바로 추가.s 
                setPlaces(prev => [...prev, data]);
            }
        })
        .catch(err => {
            console.log(err);
            alert("플레이스 생성에 실패했습니다");
        });
    };
    
    const handleMap = (e) => {
        const address = e.target.value;
        getMapfromAddress(mapRef.current, address, (map) => {
            setMarkerfromAddress(map, e.target.value)
        });
        setForm(prev => ({ ...prev, address }));
    };
    
    const handleFile = async (e) => {
        const f = e.target.files[0];
        let imageBase64 = null;
        if (f) {
            imageBase64 = await fileToBase64(f);
            setFile({ name: f.name, file: imageBase64 });
        } else setFile({});
    }

    useEffect(() => {
        if (!auth.id) navigate(URL.NOTAUTHORIZED)
    }, [])
    return <>
        <OverlayPage title="추천 플레이스">
            <form method="POST" className="d-flex flex-column" style={{ height: '100%' }} onSubmit={handleSubmit}>
                <input type="text" className="invisible position-absolute" name="userId" value={auth.id} readOnly />
                <div className="border-bottom border-gray d-flex align-items">
                    <div className="d-flex p-3 flex-grow border-right border-gray">
                        <label htmlFor="place_team" className="align-self-center pe-3 text-nowrap">팀</label>
                        <div className="select-box">
                            <select id="place_team" className="form-control" name="team" defaultValue="LG" onChange={e => setForm(prev => ({ ...prev, team: e.target.value }))}>
                                {teams.map(({ idKey, name }) => <option key={idKey} value={idKey}>{name}</option>)}
                            </select>
                        </div>
                    </div>
                    <div className="d-flex p-3 flex-grow border-right border-gray">
                        <label htmlFor="place_category" className="align-self-center pe-3 text-nowrap">카테고리</label>
                        <div className="select-box">
                            <select id="place_category" className="form-control" name="category" defaultValue="카페" onChange={e => setForm(prev => ({ ...prev, category: e.target.value }))}>
                                {categories.map((v, i) => <option key={i} value={v}>{v}</option>)}
                            </select>
                        </div>
                    </div>
                </div>
                <div className="border-bottom border-gray d-flex align-items p-3">
                    <input ref={nameRef} type="text" className="form-control border-0" id="place_name" placeholder="플레이스 이름을 입력하세요" name="title" />
                    <small hidden={!nameValid} className="text-danger error-message">{nameValid}</small>
                </div>
                <div className="border-bottom border-gray p-2 d-flex align-items-center" style={{ height: 80 }}>
                    <label htmlFor="feed_image" className="p-3 ps-4 d-flex align-items-center gap-20 pointer">
                        <i className="fas fa-camera" style={{ fontSize: 24 }} /> 
                        <p>{file?.name || "이미지를 첨부하세요 (최대 500KB)"}</p>
                    </label>
                    <input type="file" name="image" id="feed_image" className="invisible" accept="image/*" onChange={handleFile} />
                    {file?.file && <img src={file.file} alt="이미지 미리보기" className="border-radius-12 ms-auto me-2" width="auto" height="60px" />}
                </div>
                <div className="p-3 border-bottom border-gray position-relative">
                    <input type="text" ref={addressRef} className="form-control border-0" id="place_address" name="address" placeholder="상세 주소를 입력하세요" onBlur={handleMap} />
                    <small hidden={!addressValid} className="text-danger error-message">{addressValid}</small>
                </div>
                <div id="map" className="flex-grow" ref={mapRef} />
                <button className="btn btn-primary p-3 border-radius-0 mt-auto">작성하기</button>
            </form>
        </OverlayPage>
    </>
}
