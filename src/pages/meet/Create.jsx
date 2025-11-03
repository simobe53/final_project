import { useEffect, useRef, useState } from "react"
import { useNavigate, useOutletContext } from "react-router-dom";
import { OverlayPage } from "/components";
import { URL } from "/config/constants";
import { createMeet } from "/services/meets";
import { useAuth } from "/context/AuthContext";

export default function CreateMeet() {
 
    const { setMeets, setRenew } = useOutletContext();
  
    const now = new Date(Date.now() - (new Date().getTimezoneOffset() * 60000)).toISOString().substring(0,16);

    const navigate = useNavigate();
    const titleRef = useRef();
    const meetAtRef = useRef();
    const contentRef = useRef();
    const goalRef = useRef();
    const { account, name, team } = useAuth().auth; 

    //<<유효성 체크 메시지 출력을 위한 State>>
    const [titleValid, setTitleValid] = useState('');
    const [contenteValid, setContentValid] = useState('');

    //<<게시글 등록 버튼 이벤트 처리용>>
    const handleInsert = e=>{
        e.preventDefault();//제출 기능 막기
        const titleNode = titleRef.current;
        const contentNode = contentRef.current;

        if(titleNode.value.trim()===''){
            setTitleValid('제목을 입력하세요');
            titleNode.focus();
           
        }
        if(contentNode.value.trim()===''){
            setContentValid('내용을 입력하세요');
            contentNode.focus();
            
        }
        if(titleNode.value.trim()==='' || contentNode.value.trim()==='') return;   
        const title = titleNode.value;
        const content = contentNode.value;
        const goal = goalRef.current.value;
        const meetAt = meetAtRef.current.value;
        
        createMeet({ user:{ account, name }, team, title, content, meetAt, goal })
        .then(data=>{
            setRenew(prev=>!prev)
            alert("모집이 생성되었습니다");
            navigate(URL.MEET)
        })
        .catch(err=> {
            console.log(err);
            alert("모집생성에 실패했습니다.");
        })
    };
    //뒤로가기시 갱신용
    useEffect(() => {
        return () => {
            setRenew(prev=>!prev)
        }
    },[])
    
    return <>
        <OverlayPage title="모임/모집">
            <form method="POST" className="d-flex flex-column h-100">
                <div className="p-3 border-bottom border-gray">
                    <input ref={titleRef} type="text" className="form-control border-0" id="feed_title" placeholder="제목을 입력하세요" name="title"/>
                    <small hidden={!titleValid} className="text-danger error-message">{titleValid}</small>
                </div>
                <div className="flex-grow p-3 border-bottom border-gray d-flex flex-column">
                    <textarea ref={contentRef} style={{
                            resize: 'none',
                            border: 'none',
                            outline: 'none',
                            flex: '1',
                            minHeight: '220px'
                        }}  className="form-control border-0" id="feed_content" name="content" placeholder="내용을 입력하세요"></textarea>
                    <small hidden={!contenteValid} className="text-danger error-message">{contenteValid}</small>
                </div>
                <div className="border-bottom border-gray d-flex align-items">
                    <div className="d-flex p-3 flex-grow border-right border-gray">
                        <label htmlFor="feed_date" className="align-self-center pe-2 text-nowrap">일시</label>
                        <div className="flex-grow">
                            <input ref={meetAtRef} id="feed_date" type="datetime-local" name="meetAt" className="form-control" min={now} defaultValue={now} />
                        </div>
                    </div>
                    <div className="d-flex p-3" style={{ width: 260 }}>
                        <label htmlFor="feed_goal" className="align-self-center pe-2 text-nowrap">모집 인원</label>
                        <div className="flex-grow">
                            <input ref={goalRef} type="number" className="form-control" name="goal" id="feed_goal" defaultValue="1" min="1" max="99" />
                        </div>
                        <span className="align-self-center ps-2 text-nowrap">명</span>
                    </div>
                </div>
                <button className="btn btn-primary p-3 border-radius-0 mt-auto" onClick={handleInsert}>작성하기</button>
            </form>
        </OverlayPage>
    </>
}
