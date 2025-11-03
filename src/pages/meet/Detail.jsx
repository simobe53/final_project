import { useState, useEffect, useRef } from "react";
import { useLocation, useNavigate, useOutletContext, useParams } from "react-router-dom";
import { getDate, OverlayPage } from "/components";
import ProfileImg from '/components/ProfileImg';
import { createApply, createMeetComment, deleteMeet, getMeet, getMeetComment } from "/services/meets";
import { useAuth } from "/context/AuthContext";
import { URL } from "/config/constants";
import Comment from "./components/Comment";
import { acceptApply, cancleApplyMeet, closeApplyMeet, getMeetApplies, rejectApply } from "../../services/meets";
import LocationMap from "./components/LocationMap";

// const applydummy = [
//     { id: 1, account: "text01", name: "홍길동", gender: "남성" },
//     { id: 2, account: "text02", name: "김영희", gender: "여성" },
//     { id: 3, account: "text03", name: "윤두준", gender: "" },
//     { id: 4, account: "text01", name: "홍길동", gender: "남성" },
//     { id: 5, account: "text02", name: "김영희", gender: "여성" },
//     { id: 6, account: "text03", name: "윤두준", gender: "" },
// ];

export default function DetailMeet() {
    const { id } = useParams(); // id는 글의 id
    const [ meet, setMeet ] = useState({}); 
    const [ activeTab, setActiveTab ] = useState(null); // 탭 상태 추가
    const { title, content, meetAt, goal = 1, user = {}, apply:isApplied, active } = meet;
    const { profileImage = '', name: username = "윤혜리" } = user;
    const { auth } = useAuth();
    const navigate = useNavigate(); 
    const { setMeets,setRenew } = useOutletContext();
    const [ reload,setReload ] = useState(false);
    const isAuthor = user?.id === auth?.id;
    const [ comments, setComments ] = useState([]);
    const [ applies, setApplies  ] = useState([]);
    const commentRef = useRef();
    const applyCommentRef = useRef();
    const [showApplyModal, setShowApplyModal] = useState(false); // 모달 상태
    //const [applyComment, setApplyComment] = useState(""); // 신청 코멘트
    const location = useLocation();
    const tabState = location.state;

    
    // 탭 전환 핸들러
    const handleTabChange = (tab) => {
        setActiveTab(tab);
    };
    //수정버튼 클릭시
    const handleModify = (e)=>{
        navigate(`${URL.MEET}/modify/${id}`)
    }
    //삭제버튼 클릭시
    const handleDelete = (e)=>{
        deleteMeet(id)
        .then(()=>{
           // setMeets(prev=>prev.filter(m=> m.id !== parseInt(id)))
            setRenew(prev=>!prev);
            alert('삭제되었습니다.')
            navigate(URL.MEET);
        })
        .catch(err=>console.log(err.response.data.ERROR))
    }
    //id에 해당하는 글에서 댓글 작성 
    const handleInsert = (e) => {
        e.preventDefault();
        e.stopPropagation();
        if(commentRef.current.value ==='') return;  
        const commentContent  = commentRef.current?.value; 
        createMeetComment( id , { content:commentContent, user:{id:auth.id}, meet:{id:parseInt(id)} })
        .then(() => getMeetComment(id))
        .then(data=>{
            setComments(data);
            commentRef.current.value="";
        })
        .catch(err=> console.log(err.response.data.ERROR))
    }
    //댓글 삭제
    const handleCommentDelete = (commentId) => {
        //setComments(prev => prev.filter(comment=> comment.id != commentId))
        setReload(prev=>!prev)
    }
    //참여신청 버튼 누르기
    const handleApply = () => {
        setShowApplyModal(true);
    }
    // 모달 내 확인 버튼 클릭 시
    const handleApplyConfirm = () => {
        const comments = applyCommentRef.current.value;
        const apply = { comments, user:{id:auth.id}, meet:{id:parseInt(id)} }
        createApply(id,apply)
        .then(data=> {
            // 지원 생성시 지원 상태 변경 - 전체게시글 / 상세게시글
            setMeet(prev => ({...prev,apply:data.meet.apply}))
            setMeets(prev => prev.map(meet=>{ 
               return meet.id == data.meet.id ? {...meet,apply:data.meet.apply} : meet
            }))
            setReload(prev=>!prev)
        })
        .catch(err=> console.log(err.response.data.ERROR))
        setShowApplyModal(false);
        applyCommentRef.current.value="";
    }
    // 모달 내 취소 버튼 클릭 시
    const handleApplyCancel = () => {
        setShowApplyModal(false);
        applyCommentRef.current.value="";
    }
    // 지원요청 승인시
    const handleApplyAccept = (id,userId) => {
        acceptApply(id,userId)
        .then(data=>{console.log(data)
            setReload(prev=>!prev)
            //모집 상태 최신화
            setMeet(prev => ({...prev,active:data.meet.active}))
        })
        .catch(err=>{
            //에러 메세지 출력
            alert(err.response.data.ERROR)
            if(err.response.data.ERROR === "신청이 존재하지 않아요"){
            setReload(prev=>!prev)
            }
            //모집 마감
            if(err.response.data.ERROR === "정원이 가득 찼습니다."){
            setMeet(prev => ({...prev,active:false}))
            }
        })
    }
    // 지원요청 거절시
    const handleApplyReject = (id,userId) => {
        rejectApply(id,userId)
        .then(data=>{console.log(data)
            //거절시 대기 목록에서 제거
            setReload(prev=>!prev)
        })
        .catch(err=>console.log(err.response.data.ERROR))
    }
    // 모집 완료하기
    const handleCloseApply = (id) => {
        closeApplyMeet(id)
        .then(data=>{
        // 게시글 상태 변경 
            console.log(data)
            setMeet(prev => ({...prev,active:data.active}))
            setReload(prev=>!prev)  // ⭐ 추가: 참여자 목록 새로고침
        })
        .catch(err=>console.log(err.response.data.ERROR))
    }
    const handleCancleApply = () => {
        cancleApplyMeet(id,auth.id)
        .then(data=>{
            console.log(data);
            setMeet(prev=>({...prev,active:data.active,apply:data.apply}))
            setReload(prev=>!prev)
        })
        .catch(err=>console.log(err.response.data.ERROR))
    }
    useEffect(() => {
        //글id에 해당하는 글 데이터 가져오기
        getMeet(id)
        .then(data=>{
            setMeet(data);
        })
        .catch(err=> console.log(err.response.data.ERROR));
        
        // ⭐ 추가: 지원정보 초기 로드 (위치 공유 권한 체크용)
        getMeetApplies(id)
        .then(data=>{
            console.log("지원데이터 초기 로드", data)
            if(data) {
                setApplies(data)
            }
        })
        .catch(err=> console.log(err.response.data.ERROR));
        
         //뒤로가기시 갱신용
        return () => {
            setRenew(prev=>!prev)
        }
    }, []);
    //알림 (댓글) 클릭했을때 실행.
    useEffect(() => {
        setActiveTab(tabState || 'participants');
    }, [tabState]);
    useEffect(()=>{
        // comments 탭을 눌렀을때 글id에 해당하는 댓글 데이터 가져오기
        if(activeTab === "comments") {
            getMeetComment(id)
            .then(data=>{
                console.log("댓글데이터")
                setComments(data)
            })
            .catch(err=> console.log(err.response.data.ERROR));
        }
        //글id에 해당하는 지원정보 가져오기
        if(activeTab === "participants") {
            getMeetApplies(id)
            .then(data=>
            {   console.log("지원데이터",data)
                if(data) {
                    setApplies(data)
                }
            })
            .catch(err=> console.log(err.response.data.ERROR));
        }
    },[activeTab,reload])
    
    return <>
        {/* 참여신청 모달 */}
        {showApplyModal && (
            <div style={{position:'fixed',top:0,left:0,width:'100vw',height:'100vh',background:'rgba(0,0,0,0.3)',zIndex:9999,display:'flex',alignItems:'center',justifyContent:'center'}}>
                <div style={{background:'#fff',borderRadius:12,padding:24,minWidth:320,boxShadow:'0 2px 8px rgba(0,0,0,0.15)'}}>
                    <h5 className="mb-3">참여 신청</h5>
                    <div className="mb-3">
                        <label htmlFor="applyComment" className="form-label">신청 코멘트</label>
                        <input
                            id="applyComment"
                            type="text"
                            className="form-control"
                            placeholder="신청 메시지를 입력하세요"
                            ref={applyCommentRef}
                        />
                    </div>
                    <div className="mb-3">참여 신청을 하시겠습니까?</div>
                    <div className="d-flex gap-2 justify-content-end">
                        <button className="btn btn-secondary" onClick={handleApplyCancel}>취소</button>
                        <button className="btn btn-primary" onClick={handleApplyConfirm}>확인</button>
                    </div>
                </div>
            </div>
        )}
        <OverlayPage title="모임/모집" {... isAuthor && { onEdit: handleModify, onDelete: handleDelete }}>
            <div className="d-flex flex-column" style={{ height: '100%' }}>
                <div className="p-3 border-bottom border-gray d-flex gap-20 align-items-center" style={{ borderBottomWidth: '8px !important' }}>
                    <ProfileImg small src={profileImage} />
                    <div className="flex-grow">
                        <strong>{username}</strong>
                        <p className="mb-0">{title}</p>
                    </div>
                </div>
                <div className="border-bottom border-gray d-flex align-items">
                    <div className="d-flex p-3 flex-grow border-right border-gray">
                        <small className="align-self-center pe-4 text-nowrap text-gray">일시</small>
                        <small className="flex-grow">
                            {getDate(meetAt)}
                        </small>
                    </div>
                    <div className="d-flex p-3">
                        <small className="align-self-center pe-4 text-nowrap text-gray">지원 현황</small>
                        <small className="flex-grow">
                            {`${applies.length} 명 지원 / 총 ${goal} 명`}
                        </small>
                    </div>
                </div>
                <div className="flex-grow p-4 ps-3 pe-3 border-bottom border-gray" style={{ whiteSpace: 'pre-wrap' }}>
                    {content}
                </div>
         
                {/* 하단 탭바 */}
                <div className="flex-grow d-flex flex-column bg-light position-relative">
                    <div className="d-flex bg-white border-bottom border-gray">
                        <button 
                            className={`flex-fill d-flex flex-column align-items-center gap-1 pt-2 p-1 bg-transparent border-0 text-muted ${activeTab === 'participants' ? 'text-primary border-bottom border-primary border-2' : ''}`}
                            onClick={() => handleTabChange('participants')}
                        >
                            <img src="/assets/icons/empty_profile.svg" width="18" height="18" alt="지원자" />
                            <span className="small">지원자</span>
                        </button>
                        <button 
                            className={`flex-fill d-flex flex-column align-items-center gap-1 p-1 pt-2 bg-transparent border-0 text-muted ${activeTab === 'location' ? 'text-primary border-bottom border-primary border-2' : ''}`}
                            onClick={() => handleTabChange('location')}
                        >
                            <i className="fas fa-map-marker-alt" style={{ fontSize: '18px' }}></i>
                            <span className="small">위치</span>
                        </button>
                        <button 
                            className={`flex-fill d-flex flex-column align-items-center gap-1 p-1 pt-2 bg-transparent border-0 text-muted ${activeTab === 'comments' ? 'text-primary border-bottom border-primary border-2' : ''}`}
                            onClick={() => handleTabChange('comments')}
                        >
                            <img src="/assets/icons/comment.png" width="18" height="18" alt="댓글" />
                            <span className="small">댓글</span>
                        </button>
                    </div>
                    
                    {/* 지원자 탭 내용 */}
                    <div className={`flex-grow overflow-auto ${activeTab === 'participants' ? 'd-flex' : 'd-none'}`} style={{ height: '200px' }}>
                        <div className="flex-grow d-flex flex-column">
                            <div className="d-flex justify-content-between align-items-center p-3 bg-white border-bottom">
                                <h6 className="mb-0 fw-semibold">참여자</h6>
                                <span className="small">{applies.filter(d => d.status == "PENDING").length}명 대기 / {applies.filter(d => d.status == "APPROVED").length}명 참여 / 총 {goal}명</span>
                            </div>
                            <div className="flex-grow">
                                {/*(모집 마감전) 작성자일 경우 대기자 목록 / 작성자가 아닐경우 본인의 신청상태*/}
                                { active &&
                                    <>  
                                            <div className="list-group">
                                                {applies.filter(d => d.status == "PENDING").filter(u => isAuthor ?  u : u.user.id == auth.id ).map(({ id:applyId,comments,user }) => 
                                                    <div key={applyId}> 
                                                        <div className="p-2 ps-3 pe-3 bg-white border-bottom border-gray d-flex align-items-center justify-content-between">
                                                            <div className="d-flex align-items-center gap-2">
                                                                <ProfileImg src={user.profileImage} zoom={0.45} />
                                                                <div className="d-flex flex-column">
                                                                    <div className="d-flex align-items-center gap-2">
                                                                        <div className="fw-semibold ms-2">{user.name}</div>
                                                                        <span className="small" style={{ opacity: 0.6 }}>
                                                                            {user.gender == '남자' && <i className="text-primary fa-solid fa-mars"> 남성</i>}
                                                                            {user.gender == '여자' && <i className="text-danger fa-solid fa-venus"> 여성</i>}
                                                                        </span>
                                                                        </div>
                                                                    <div className="ms-2" style={{fontSize: '14px' }} >
                                                                    코멘트 : {comments || "잘 부탁 드려요"}
                                                                    </div>
                                                                </div>
                                                            </div>
                                                            <div className="d-flex gap-2">
                                                            { isAuthor ? 
                                                                (<>
                                                                <button className="btn btn-outline-primary btn-sm rounded-pill px-3" onClick={()=>handleApplyAccept(id,user.id)}>수락</button>
                                                                <button className="btn btn-outline-danger btn-sm rounded-pill px-3" onClick={()=>handleApplyReject(id,user.id)}>거절</button>
                                                                </> 
                                                                ):
                                                                (
                                                                (user.id == auth.id) && <span className="btn btn-outline-danger btn-sm rounded-pill px-3" onClick={handleCancleApply} >신청취소</span>
                                                                )
                                                            }
                                                            </div>
                                                        </div>
                                                    </div>
                                                )}
                                            </div>
                                    </>
                                }
                                {/*(모집 마감 상관없이),모두에게  요청 승인받은 참가자 목록 */}
                                    <div className="list-group">
                                        {applies.filter(d => d.status == "APPROVED").map(({ id:applyId,comments,user }) => 
                                            <div key={applyId}>
                                                <div className="p-2 ps-3 pe-3 bg-white border-bottom border-gray d-flex align-items-center justify-content-between">
                                                        <div className="d-flex align-items-center gap-2">
                                                                <ProfileImg src={user.profileImage} zoom={0.45} />
                                                                <div className="d-flex flex-column">
                                                                    <div className="d-flex align-items-center gap-2">
                                                                        <div className="fw-semibold ms-2">{user.name}</div>
                                                                        <span className="small" style={{ opacity: 0.6 }}>
                                                                            {user.gender == '남자' && <i className="text-primary fa-solid fa-mars"> 남성</i>}
                                                                            {user.gender == '여자' && <i className="text-danger fa-solid fa-venus"> 여성</i>}
                                                                        </span>
                                                                        </div>
                                                                    <div className="ms-2" style={{fontSize: '14px' }} >
                                                                    코멘트 : {comments || "잘 부탁 드려요"}
                                                                    </div>
                                                                </div>
                                                            </div>
                                                    <div className="d-flex gap-2">
                                                        { (auth.id == user.id) && 
                                                            <span className="btn btn-outline-danger btn-sm rounded-pill px-3" onClick={handleCancleApply} >참가취소</span>
                                                        }
                                                    </div>
                                                </div>
                                            </div>
                                        )}          
                                    </div>
                                {/*(모집 마감전) 지원/참여자가 없을때 */}
                                { active&&
                                    <>
                                        {isAuthor && !( applies.length > 0) && (
                                            <div className="d-flex flex-column align-items-center justify-content-center h-100 text-muted text-center">
                                                <img src="/assets/icons/empty_profile.svg" width="40" height="40" alt="지원자 없음" className="opacity-50 mb-3" />
                                                <p className="mb-1 fw-medium">아직 지원자가 없습니다.</p>
                                            </div>
                                        )}
                                        {!isAuthor && !( applies.filter(d => d.status == "APPROVED")?.length > 0 || applies.filter(d => d.status == "PENDING").filter(u => u.user.id == auth.id ).length > 0) && !(applies.filter(d => d.status == "REJECTED").filter(u => u.user.id === auth.id).length > 0) &&(
                                            <div className="d-flex flex-column align-items-center justify-content-center h-100 text-muted text-center">
                                                <img src="/assets/icons/empty_profile.svg" width="40" height="40" alt="지원자 없음" className="opacity-50 mb-3" />
                                                <p className="mb-1 fw-medium">아직 참가자가 없습니다.</p>
                                                <small>참가신청을 해보세요!</small>
                                            </div>
                                        )}   
                                    </>
                                }
                            </div>
                        </div>
                    </div>
                    
                    {/* 위치 공유 탭 내용 */}
                    <div className={`flex-grow ${activeTab === 'location' ? 'd-block' : 'd-none'}`} style={{ height: '500px' }}>
                        {/* 작성자 또는 승인된 참가자만 위치 공유 가능 */}
                        {isAuthor || applies.some(a => a.user.id === auth.id && a.status === 'APPROVED') ? (
                            <LocationMap 
                                meetId={parseInt(id)} 
                                participants={[
                                    // 작성자 추가 (작성자는 항상 포함)
                                    ...(user?.id ? [{ user, status: 'APPROVED' }] : []),
                                    // 승인된 참가자들
                                    ...applies.filter(a => a.status === 'APPROVED')
                                ]}
                            />
                        ) : (
                            <div className="d-flex flex-column align-items-center justify-content-center h-100 text-muted text-center p-4">
                                <i className="fas fa-lock mb-3" style={{ fontSize: '40px', opacity: 0.5 }}></i>
                                <p className="mb-1 fw-medium">위치 공유는 승인된 참가자만 이용할 수 있습니다</p>
                                <small className="opacity-75">모임에 참가 신청하고 승인을 받아주세요</small>
                            </div>
                        )}
                    </div>
                    
                    {/* 댓글 탭 내용 */}
                    <div className={`flex-grow overflow-auto ${activeTab === 'comments' ? 'd-flex' : 'd-none'}`} style={{ height: '200px' }}>
                        <div className="flex-grow d-flex flex-column">
                            <div className="d-flex justify-content-between align-items-center p-3 bg-white border-bottom">
                                <h6 className="mb-0 fw-semibold">댓글</h6>
                                <span className="small">{comments.length}개</span>
                            </div>
                            <div className="flex-grow">
                            { comments.length != 0 &&  
                            <>
                                <ul className="list-group full">
                                    {comments.map(comment => (
                                        <Comment 
                                            key={comment.id} 
                                            {...comment} 
                                            onCommentDeleted={handleCommentDelete}
                                        />
                                    ))}
                                </ul>
                                <div style={{ height: 60 }} /> 
                            </>                                 }       
                            { comments.length == 0 && 
                                <div className="d-flex flex-column align-items-center justify-content-center h-100 text-muted text-center">
                                    <img src="/assets/icons/comment.png" width="40" height="40" alt="댓글 없음" className="opacity-50 mb-3" />
                                    <p className="mb-1 fw-medium">아직 댓글이 없습니다.</p>
                                    <small className="opacity-75">첫 번째 댓글을 남겨보세요!</small>
                                </div>  
                            }
                            </div>
                        </div>
                    </div>
                </div>
                {/* 하단 액션 바 */}
                <div className={`d-flex align-items-stretch justify-content-stretch bg-white border-top shadow-sm ${activeTab === 'comments' || activeTab === 'location' ? 'd-none' : ''}`}>
                {!auth?.id ? (
                    // 비로그인 유저
                    <button
                        className="btn btn-secondary border-radius-0 p-3 w-100 fw-semibold shadow"
                        style={{ minWidth: '400px' }}
                        onClick={() => navigate('/login')}
                    >
                        로그인이 필요합니다
                    </button>
                ) : active ? (
                    isAuthor ? (
                        // 작성자일때 모집 완료 하기 버튼
                        <button
                        className="btn btn-primary border-radius-0 p-3 w-100 fw-semibold shadow"
                        onClick={() => handleCloseApply(id)}
                        >
                        모집 완료 하기
                        </button>
                        ) : (
                        // 작성자가 아닐때, 거절당한 유저에게 보임
                        applies.filter(d => d.status == "REJECTED").filter(u => u.user.id === auth.id).length > 0 ? (
                        //
                        <button
                            className="btn btn-danger border-radius-0 p-3 w-100 fw-semibold shadow"
                            style={{ minWidth: '400px' }}
                            disabled
                        >
                            거절되었습니다.
                        </button>
                        ) : (
                        // 작성자가 아닐때, 참여 신청한 유저 / 신청안한 유저
                        <button
                            className="btn btn-primary border-radius-0 p-3 w-100 fw-semibold shadow"
                            onClick={handleApply}
                            style={{ minWidth: '400px' }}
                            disabled={isApplied}
                        >
                            {isApplied ? applies.filter(d => d.status == "APPROVED").filter(u => u.user.id == auth.id ).length > 0 ?'승인되었습니다' : '참여 신청을 보냈습니다' : '참여 신청하기'}
                        </button>
                        )
                    )
                    ) : (
                    // 모집 마감 상태
                    <button
                        className="btn btn-primary border-radius-0 p-3 w-100 fw-semibold shadow"
                        style={{ minWidth: '400px' }}
                        disabled
                    >
                        모집 마감
                    </button>
                    )}

                </div>
                {/* 댓글 입력창 - 독립적으로 표시 */}
                <div className={`p-2 bg-white border-top shadow-sm ${activeTab === 'comments' ? 'd-block' : 'd-none'}`} style={{ height: '60px', boxSizing: 'border-box' }}>
                    {!auth?.id ? (
                        // 비로그인 유저
                        <div className="d-flex gap-2 align-items-center h-100 justify-content-center text-muted">
                            <i className="fas fa-lock"></i>
                            <span>댓글을 작성하려면 로그인이 필요합니다</span>
                        </div>
                    ) : (
                        // 로그인 유저
                        <form className="d-flex gap-2 align-items-center h-100" onSubmit={handleInsert}>
                            <input
                                ref={commentRef}
                                type="text"
                                className="form-control rounded-pill"
                                placeholder="댓글을 입력하세요..."
                                id="content"
                            />
                            <button type="submit" className="btn btn-primary rounded-circle d-flex align-items-center justify-content-center" style={{ width: '40px', height: '40px' }}>
                                <i className="fas fa-paper-plane"></i>
                            </button>
                        </form>
                    )}
                </div>

            </div>
        </OverlayPage>
    </>
}
