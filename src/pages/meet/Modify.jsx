import { useEffect, useRef, useState } from "react"
import { useNavigate, useOutletContext, useParams } from "react-router-dom";
import { OverlayPage } from "/components";
import { URL } from "/config/constants";
import { getMeet, updateMeet } from "/services/meets";
import { useAuth } from "/context/AuthContext";

export default function ModifyMeet() {
    
    const now = new Date(Date.now() - (new Date().getTimezoneOffset() * 60000)).toISOString().substring(0,16);
    //Detail에서  navigate로 넘겨온 파라미터,  글 id값을 구조분해로 받음
    const { id } = useParams();
    const [ minGoal, setMinGoal] = useState(1);
    /** 
     *  [혜리 comment] meets를 Outlet에서 받는 이유가 뭐죠?! 
     *  목록 변경할때 사용하기 위해서라면 setMeets에서 이전 state 값을 받을 수 있습니다!
     *  setMeets(prev => {}) 여기서 prev가 meets 값이 됨.
     **/
    const { setMeets, setRenew } = useOutletContext();

    const [meet, setMeet] = useState({
        title: '',
        content: '',
        meetAt: now,
        createdAt: now,
        goal: 1,
        user: null
    }); 
    //구조분해 해주고 
    const { title , content, meetAt, createdAt , goal = 1, user } = meet;
    
    //로그인 유저와 저자가 같은지 체크 (둘다 null이면 안됨)
    const { auth } = useAuth();
    const isAuthor = user?.id === auth?.id;
    
    // useRef로 폼 값을 받아옴.
    const titleRef = useRef();
    const contentRef = useRef();
    const goalRef = useRef();
    const navigate = useNavigate();

    //<<유효성 체크 메시지 출력을 위한 State>>
    const [titleValid, setTitleValid] = useState('');
    const [contenteValid, setContentValid] = useState('');
    //<<>>
    const handleForm = e=>{
        const {name,value} = e.target; 
        setMeet(prev=>({...prev,[name]:value}))
        //실시간 유효성 체크도 가능하다
    }

    //<<게시글 수정 버튼 이벤트 처리용>>
    const handleModify = e=>{
        e.preventDefault();//제출 기능 막기
        const titleNode = titleRef.current;
        const contentNode = contentRef.current;
        const goalNode = goalRef.current;
        if(titleNode.value.trim()===''){
            setTitleValid('제목을 입력하세요');
            titleNode.focus();
        }

        if(contentNode.value.trim()===''){
            setContentValid('내용을 입력하세요');
            contentNode.focus();
        }
        if(goalNode.value < minGoal ){
            goalNode.value = minGoal;
            goalNode.focus();
            alert(`모집 인원이 참가자( ${minGoal}명 )보다 적을 수 없습니다`);
            return;
        }

        if(titleNode.value.trim()==='' || contentNode.value.trim()==='') return;   
        
        updateMeet({...meet,title:title,content:content,goal:goal})
        .then(data=>{
            //글 수정, 상태 함수 작성. 
          //  setMeets(prev => prev.map(m => data.id === m.id ? data : m));
            setRenew(prev=>!prev)
            alert("모집이 수정되었습니다");
            navigate(`${URL.MEET}/${data.id}`)
        })
        .catch(err=> {
            console.log(err);
            alert("모집 수정에 실패했습니다.");
        })
    };
    //초기 렌더링.
    useEffect(e => {
        getMeet(id)
        .then((data)=>{
            if(data.approved){
                setMinGoal(data.approved)
            }
            setMeet(data);
        })
        .catch(err=> {
            console.log(err);
            alert("(수정)글을 가져올 수 없습니다.");
        })
         //뒤로가기시 갱신용
        return () => {
            setRenew(prev=>!prev)
        }
    },[])

    return <>
        <OverlayPage title="수정">
            <form method="POST" className="d-flex flex-column h-100">
                <div className="p-3 border-bottom border-gray">
                    <input ref={titleRef} value={title} onChange={handleForm} type="text" className="form-control border-0" id="feed_title" placeholder="제목을 입력하세요" name="title"/>
                    <small className="text-danger">{titleValid}</small>
                </div>
                <div className="flex-grow p-3 border-bottom border-gray d-flex flex-column">
                    <textarea 
                        ref={contentRef} 
                        value={content} 
                        onChange={handleForm} 
                        style={{
                            resize: 'none',
                            border: 'none',
                            outline: 'none',
                            flex: '1',
                            minHeight: '220px'
                        }} 
                        className="form-control border-0" 
                        id="feed_content" 
                        name="content" 
                        placeholder="내용을 입력하세요"
                    />
                    <small className="text-danger mt-2">{contenteValid}</small>
                </div>
                <div className="border-bottom border-gray d-flex align-items">
                    <div className="d-flex p-3 flex-grow border-right border-gray">
                        <label htmlFor="feed_date" className="align-self-center pe-2 text-nowrap">일시</label>
                        <div className="flex-grow">
                            <input id="feed_date" type="datetime-local" name="meetAt" className="form-control" min={now} value={meetAt} onChange={handleForm} />
                        </div>
                    </div>
                    <div className="d-flex p-3" style={{ width: 260 }}>
                        <label htmlFor="feed_goal" className="align-self-center pe-2 text-nowrap">모집 인원</label>
                        <div className="flex-grow">
                            <input ref={goalRef} value={goal} onChange={handleForm} type="number" className="form-control" name="goal" id="feed_goal" min={minGoal} max="99"/>
                        </div>
                        <span className="align-self-center ps-2 text-nowrap">명</span>
                    </div>
                </div>
                { isAuthor &&
                <button className="btn btn-primary p-3 border-radius-0 mt-auto" onClick={handleModify}>수정 하기</button>
                }
            </form>
        </OverlayPage>
    </>
}