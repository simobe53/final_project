import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { URL, REGEX } from "/config/constants";
import Input from "/components/Input";
import Statusbar from '/components/StatusBar';
import { fileToBase64 } from "/components/File";
import { getUserByAccount, createUser, mailCheck, sendEmail } from "/services/users";
import SignupTeam from "./SignupTeam";
import classes from "./Signup.module.scss";

export default function Signup() {
    const [step, setStep] = useState(0);
    const [fileName, setFileName] = useState('');
    const [form, setForm] = useState({
        email: '',
        account: '',
        accountConfirm: undefined,  // undefined: 중복체크 미진행, false : 중복체크 통과못함, true : 중복체크 통과
        password: '',
        passwordConfirm: '',
        name: '',
        team: {},
        gender: '',
        profileImage: null
    });
    const { account, email, accountConfirm, password, passwordConfirm, name, team, gender, profileImage } = form;
    const [emailVerified, setEmailVerified] = useState(null);
    const [verificationCode, setVerificationCode] = useState('');
    const [verificationStatus, setVerificationStatus] = useState('');
    const [remainingTime, setRemainingTime] = useState(0);
    const [timerId, setTimerId] = useState(null);
    const validate = account && password && name && team?.id && passwordConfirm && password === passwordConfirm && RegExp(REGEX.ACCOUNT).test(account) && RegExp(REGEX.PASSWORD).test(password) && email && emailVerified;
    const navigate = useNavigate();

    const checkDuplicate = () => {
        getUserByAccount(account)
        .then(user => setForm(prev => ({ ...prev, accountConfirm: user?.id ? false : true })))
        .catch(() => setForm(prev => ({ ...prev, accountConfirm: false })))
    }

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!accountConfirm) {
            alert("아이디 중복 체크를 진행해주세요.");
            return;
        }
        if (!validate) {
            alert("모든 항목을 제대로 입력해주세요.");
            return;
        }

        createUser({ account, password, name, email, team, gender, profileImage })
        .then(() => {
            alert("회원가입이 완료되었습니다. 로그인 페이지로 이동합니다.");
            navigate(URL.LOGIN); //해당 URL로 이동
        }).catch(() => alert("회원가입에 실패했습니다."))
    };


    const handleFile = async e => {
        const { files } = e.target;
        const [file] = files;
        if (!file) return;  // 파일없음
        if (file?.size / 1024 > 500) {  // 500KB 초과
            alert("파일 사이즈는 500KB를 초과할 수 없습니다.");
            e.target.files = null;
            setFileName('');
            setForm(prev => ({ ...prev, profileImage: null }));
            return;
        }
        const base64 = await fileToBase64(file);
        setFileName(file.name);
        setForm(prev => ({ ...prev, profileImage: base64 }));
    };

    const onBack = e => {
        e.preventDefault();
        setForm(prev => ({ ...prev, team: {} }));
    }

    useEffect(() => setStep(form.team?.id ? 1 : 0), [form.team]);

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

    return <>
        <div className={classes.container}>
            <form className={classes.form} style={{ left: `-${step * 100}%`}} onSubmit={handleSubmit} >
                <SignupTeam onConfirm={(v) => setForm(prev => ({ ...prev, team: v }))} onComplete={() => setStep(1)} />
                <div className="d-flex flex-column align-items-stretch overflow-y-auto">
                    <Statusbar title="회원가입" onBack={onBack} />
                    <div className="d-flex flex-column flex-grow align-items-stretch gap-20 p-4" style={{ marginTop: 68 }}>
                        <div className="d-flex align-items-center">
                            <Input
                                id="register_id"
                                type="text"
                                name="account"
                                label="아이디"
                                required
                                value={form.account}
                                onChange={e => setForm(prev => ({ ...prev, account: e.target.value, accountConfirm: undefined }))}
                                errorMessage={accountConfirm == false ? '중복된 아이디가 있습니다.' : ''}
                            />
                            <div>
                                <input 
                                    type="button"
                                    value={accountConfirm ? '사용가능' : '중복확인'} 
                                    className={`btn border border-gray border-radius-20 ${accountConfirm == true ? classes.identified : ''}`} 
                                    disabled={!!accountConfirm} 
                                    onClick={checkDuplicate} 
                                />
                             </div>
                        </div>
                        
                        <Input
                            id="register_password"
                            type="password"
                            name="password"
                            label="비밀번호"
                            required
                            value={form.password}
                            onChange={e => setForm(prev => ({ ...prev, password: e.target.value }))}
                        />

                        <Input
                            label="비밀번호 확인"
                            placeholder="비밀번호를 다시 한번 입력하세요"
                            id="register_password_confirm"
                            type="password"
                            required
                            errorMessage={password && passwordConfirm && password != passwordConfirm ? '입력하신 비밀번호와 일치하지 않습니다' : ''}
                            value={form.passwordConfirm}
                            onChange={e => setForm(prev => ({ ...prev, passwordConfirm: e.target.value }))}
                        />
                        <Input 
                            id="register_name"
                            type="text"
                            name="name"
                            label="이름"
                            placeholder="이름을 입력하세요"
                            required
                            value={form.name}
                            onChange={e => setForm(prev => ({ ...prev, name: e.target.value }))}
                        />
                        <div className="d-flex align-items-center">
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
                            <div className="d-flex align-items-center mb-2" style={{ marginTop: -8 }}>
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
                        <hr />
                        <div className="d-flex flex-column align-items-stretch gap-8">
                            <small>성별</small>
                            <fieldset className="d-flex">
                                <input type="radio" id="male" value="남자" name="gender" onChange={() => setForm(prev => ({ ...prev, gender: '남자' }))} />
                                <label htmlFor="male" style={{ height: 60 }}>남자</label>
                                <input type="radio" id="female" value="여자" name="gender" onChange={() => setForm(prev => ({ ...prev, gender: '여자' }))} />
                                <label htmlFor="female" style={{ height: 60 }}>여자</label>
                            </fieldset>
                        </div>
                        <div className="d-flex align-items-center gap-8">
                            <div className="d-flex flex-grow flex-column align-items-stretch gap-8">
                                <small>프로필 사진</small>
                                <input id="profile_image" type="file" name="profileImage" accept="image/jpg, image/png, image/jpeg" onChange={handleFile} />
                                <label htmlFor="profile_image">
                                    <i className="fas fa-camera" />
                                    <p>{fileName || '이미지를 첨부하세요 (최대 500KB)'}</p>
                                </label>
                            </div>
                            {profileImage && <img src={profileImage} width="90px" height="90px" className="border border-gray border-radius-20" />}
                        </div>

                    </div>
                    <button type="submit" className="btn btn-primary border-radius-0 mt-auto" disabled={!validate}>{validate ? '회원가입' : '필수 사항을 전부 입력하세요'}</button>
                </div>
            </form>
        </div>
    </>;
}
