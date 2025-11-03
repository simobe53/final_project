import { useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { URL } from "/config/constants";
import { useInit } from "/context/InitContext";
import { useAuth } from "/context/AuthContext";
import StatusBar from "/components/StatusBar";
import TeamSelect from "/components/TeamSelect";
import Input from "/components/Input";
import { fileToBase64 } from "/components/File";
import { updateUser, getUserByAccount, mailCheck, sendEmail } from "/services/users";
import ResignBtn from "/pages/login/ResignBtn";
import PwdChangeBtn from "/pages/login/PwdChangeBtn";
import classes from "/pages/login/Signup.module.scss";


export default function MyPageUserInfo() {
    const { auth, fetchAuth } = useAuth();
    const { teams } = useInit();
    const [filename, setFileName] = useState('');
    const [showMores, setShowMores] = useState(false);
    const [form, setForm] = useState(auth);
    const { account, name, team, gender, profileImage, email } = form;
    const [emailVerified, setEmailVerified] = useState(null);
    const [verificationCode, setVerificationCode] = useState('');
    const [verificationStatus, setVerificationStatus] = useState('');
    const [remainingTime, setRemainingTime] = useState(0);
    const [timerId, setTimerId] = useState(null);

    const validate = name && team && emailVerified !== false;
    const isSocialLogin = !isNaN(auth.account);

    const navigate = useNavigate();
    const toggleMores = () => setShowMores(prev => !prev);

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!validate) {
            alert("필수 항목을 입력해주세요.");
            return;
        }

        updateUser({ ...form, name, email, team, gender, profileImage })
        .then(user => {
            fetchAuth(user);    // context 정보 업데이트
            alert("정보 수정이 완료되었습니다. 마이 페이지로 이동합니다.");
            navigate(URL.MYPAGE);   // 해당 URL로 이동
        }).catch(() => alert("회원 정보 수정에 실패했습니다."))
    };


    const handleFile = async e => {
        const { files } = e.target;
        const [file] = files;
        if (!file) return;  // 파일없음
        if (file?.size / 1024 > 500) {  // 500KB 초과
            alert("파일 사이즈는 500KB를 초과할 수 없습니다.");
            e.target.files = null;
            setFileName('');
            setForm(prev => ({ ...prev, profileImage: auth.profileImage }));
            return;
        }
        const base64 = await fileToBase64(file);
        setFileName(file.name);
        setForm(prev => ({ ...prev, profileImage: base64 }));
    };

    const sendEmailVerification = async () => {
        if (!email) {
            alert("이메일을 입력해주세요.");
            return;
        }

        sendEmail(email)
        .then(data => {
            if (data.success) {
                alert("인증 이메일이 발송되었습니다.");
                setRemainingTime(180);
                startCountdown();
                setEmailVerified(false); // 인증코드 입력창 활성화
            } else {
                alert("이메일 발송 실패: " + data.error || '알 수 없음');
            }
        })
        .catch(err => {
            console.error(err);
            alert("오류 발생: " + err.message);
        })
    };

    const startCountdown = () => {
        if (timerId) clearInterval(timerId);
        const id = setInterval(() => {
            setRemainingTime(prev => {
                if (prev <= 1) {
                    clearInterval(id);
                    setVerificationStatus("인증 시간이 만료되었습니다.");
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);
        setTimerId(id);
    };

    const verifyCode = async () => {
        if (!verificationCode) {
            setVerificationStatus("인증 코드를 입력해주세요.");
            return;
        }

        mailCheck(verificationCode)
        .then(isMatch => {
            if (isMatch) {
                clearInterval(timerId);
                setRemainingTime(0);
                setVerificationStatus("");
                setEmailVerified(true);
            } else {
                setVerificationStatus("인증 코드가 올바르지 않습니다.");
            }
        })
        .catch(err => {
            console.error(err);
            setVerificationStatus("인증 확인 중 오류가 발생했습니다.");
        })
    };

    const minutes = Math.floor(remainingTime / 60);
    const seconds = remainingTime % 60;    

    useEffect(() => {
        if (!account) return;
        getUserByAccount(account).then(data => {
            if (data?.id) setForm(data);
        });
    }, [account]);

    // if (!form.id) return null;
    return <>
        <StatusBar title="회원 정보 수정">
            <button className="btn btn-none border-radius-20 p-0" onClick={toggleMores}>
                <i className="fa-solid fa-ellipsis"></i>
            </button>
        </StatusBar>
        <form className="overflow-y-auto d-flex flex-column align-items-stretch" onSubmit={handleSubmit} style={{ height: '100%' }}>
            <div className="d-flex flex-column flex-grow align-items-stretch gap-20 p-4 mt-3">
                <input type="text" name="id" value={form.id} hidden readOnly />
                <div className="d-flex align-items-center gap-8 mb-4">
                    <div className="d-flex flex-grow flex-column align-items-stretch gap-8">
                        <small>응원 팀</small>
                        <div className="d-flex gap-20 flex-wrap" style={{ zoom: 0.6 }}>
                            {teams.map(t =>                              
                                <TeamSelect key={t.id} size="small" team={t} selected={team} setTeam={tm => setForm(prev => ({ ...prev, team: tm }))} />
                            )}
                        </div>
                    </div>
                </div>
                <Input 
                    id="user_name"
                    type="text"
                    name="name"
                    label="이름"
                    placeholder="이름을 입력하세요"
                    required
                    value={form.name}
                    onChange={e => setForm(prev => ({ ...prev, name: e.target.value }))}
                />
                <div className="d-flex align-items-center gap-8 mt-3">
                    <Input
                        id="register_email"
                        type="email"
                        label="이메일"
                        placeholder="이메일을 입력하세요"
                        required
                        value={email}
                        onChange={e => setForm(prev => ({ ...prev, email: e.target.value }))}
                    />
                    <input
                        type="button"
                        className={`btn border border-gray border-radius-20 ${emailVerified ? classes.identified : ''}`}
                        value={emailVerified ? "인증 완료" : "이메일 인증"}
                        onClick={sendEmailVerification}
                        disabled={emailVerified}
                    />
                </div>
                {emailVerified === false && <>
                    <div className="d-flex align-items-center mb-2 gap-8" style={{ marginTop: -20 }}>
                        <Input
                            id="email_verify_code"
                            type="text"
                            label="인증 코드"
                            placeholder="인증 코드 입력"
                            value={verificationCode}
                            onChange={e => setVerificationCode(e.target.value)}
                            errorMessage={verificationStatus || (remainingTime > 0 && `남은 인증 시간: ${minutes}:${seconds < 10 ? '0' : ''}${seconds}`)}
                        />
                        <input
                            type="button"
                            className="btn border border-gray border-radius-20"
                            value="확인"
                            onClick={verifyCode}
                        />
                    </div>
                </>}
                <div className="d-flex align-items-center gap-8">
                    <div className="d-flex flex-grow flex-column align-items-stretch gap-8">
                        <small>프로필 사진</small>
                        <input id="profile_image" type="file" name="profileImage" accept="image/jpg, image/png, image/jpeg" onChange={handleFile} />
                        <label htmlFor="profile_image">
                            <i className="fas fa-camera" />
                            <p>{filename || '이미지를 첨부하세요 (최대 500KB)'}</p>
                        </label>
                    </div>
                    {profileImage && <img src={profileImage} width="90px" height="90px" className="border border-gray border-radius-20" />}
                </div>
                <div className="d-flex flex-column align-items-stretch gap-8">
                    <small>성별</small>
                    <fieldset className="d-flex">
                        <input type="radio" id="male" value="남자" name="gender" checked={gender === '남자'} onChange={() => setForm(prev => ({ ...prev, gender: '남자' }))} />
                        <label htmlFor="male" style={{ height: 60 }}>남자</label>
                        <input type="radio" id="female" value="여자" name="gender" checked={gender === '여자'} onChange={() => setForm(prev => ({ ...prev, gender: '여자' }))} />
                        <label htmlFor="female" style={{ height: 60 }}>여자</label>
                    </fieldset>
                </div>
            </div>
            <button type="submit" className="btn btn-primary border-radius-0 mt-auto" style={{ height: 60, minHeight: 60 }} disabled={!validate}>{validate ? '회원 정보 수정' : '필수 사항을 전부 입력하세요'}</button>
        </form>
        {showMores && <>
            <div className="position-absolute" style={{ width: '100%', height: '100%', top: 59, right: 0, zIndex: 1 }}>
                <div role="button" onClick={toggleMores} className="position-absolute" style={{ width: '100%', height: '100%', background: 'rgba(0,0,0,0.5)' }} />
                <ul className="list-group bg-white border-radius-0 position-absolute"  style={{ width: '100%', zIndex: 1 }}>
                    <li className="d-flex list-group-item p-0" style={{ height: 60 }}>
                        <ResignBtn />
                    </li>
                    {/* 소셜 로그인은 비밀번호 변경 기능 제공 안한다 */}
                    {!isSocialLogin && <>
                        <li className="d-flex list-group-item p-0" style={{ height: 60 }}>
                            <PwdChangeBtn />
                        </li>
                    </>}
                </ul>
            </div>
        </>}
    </>;
}