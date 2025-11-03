import { useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "/context/AuthContext"
import { useInit } from "/context/InitContext"
import { OverlayPage } from "/components";
import LoadingAI from "/components/LoadingAI";
import { URL, teams as teaminfos } from "/config/constants";
import { verifyTicket, createDiary } from "/services/diary";
import InputTicket from "./InputTicket";
import classes from './Create.module.scss';

export default function Create() {
    const { auth } = useAuth();
    const { teams } = useInit();
    const [loading, setLoading] = useState(false);
    const [step, setStep] = useState(0);
    const [ticket, setTicket] = useState(null);
    const [ticketInfo, setTicketInfo] = useState(null);
    const [images, setImages] = useState([]);
    const imagesRef = useRef(null);
    const navigate = useNavigate();

    const handleImagesChange = (e) => {
        const files = Array.from(e.target.files);
        files.forEach(file => {
            const reader = new FileReader();
            reader.onload = (ev) => {
                const base64 = ev.target.result;
                setImages(prev => [...prev, { file, base64 }]);
            };
            reader.readAsDataURL(file);
        });
    };

    const removeImage = (index) => {
        setImages(prev => prev.filter((_, i) => i !== index));
    };

    const handleSubmit =  (e) => {
        e.preventDefault();
        const photoBase64List = images.map(img => img.base64);
        setLoading(true);
        const diaryData = {
            ticket_data: ticketInfo,
            photo_base64_list: photoBase64List,
        }
        console.log("📌 React: 보내는 payload", diaryData);
        createDiary(diaryData)
            .then((data) => {
                setLoading(false);
                console.log("✅ React: createDiary 응답:", data);
                navigate(`${URL.DIARY}?date=${data.date||ticketInfo.date}`);
            })
            .catch((e) => {
                setLoading(false);
                console.error("❌ React: createDiary 에러:", e);
                alert("생성에 실패하였습니다!");
            });
    };

    // 티켓 검증 메소드
    const getTicketInfo = (fileBase64) => {
        setTicket(fileBase64);
        setLoading(true);
        verifyTicket({ ticket_base64: fileBase64 })
            .then(({ is_ticket, qr_present, ...data }) => {
                setLoading(false);
                if (!is_ticket|| !qr_present) {
                    alert("유효한 티켓이 아닙니다!");
                    setStep(0);
                } else {
                    const { home_team, away_team, ...info } = data;
                    const hometeam = teams.find(({ name }) => name.includes(home_team));
                    const awayteam = teams.find(({ name }) => name.includes(away_team));
                    const where = teaminfos.find(({ img }) => img === hometeam.idKey)?.home;
                    setTicketInfo({ ...info, match: `${hometeam.name} VS ${awayteam.name}`, where });
                    setStep(2);
                }
            })
            .catch((e) => {
                console.error("❌ 티켓 검증 오류:", e);
                setLoading(false);
                alert("검증 과정에서 오류가 있습니다.");
            });
    };

    return <>
        <OverlayPage title="직관일기 작성">
            <form onSubmit={handleSubmit} className="d-flex flex-column full-height">
                {/* 티켓 이미지 첨부 */}
                {step !== 2 && <InputTicket onChange={getTicketInfo} step={step} setStep={setStep} />}

                {step === 2 && <>
                    {/* 티켓 이미지 미리보기 및 불러온 정보 */}
                    {ticketInfo && <>
                        <div className={classes.imageSection}>
                            <p className={classes.imageLabel}>티켓 사진</p>
                            <div className="d-flex gap-20" style={{ flexGrow: 1 }}>
                                <img src={ticket} className="border-radius-12" alt="직관 티켓 사진" width="200px" />
                                <div className="d-flex flex-column gap-8 pt-2 ps-2" style={{ flexGrow: 1 }}>
                                    <div className="mb-2">
                                        <label for="date" className="ps-1 h6 mb-2">일자</label>
                                        <input id="date" className="form-control p-2 ps-3" type="text" readOnly value={ticketInfo.date} />
                                    </div>
                                    <div className="mb-2">
                                        <label for="match" className="ps-1 h6 mb-2">경기</label>
                                        <input id="match" className="form-control p-2 ps-3" type="text" readOnly value={ticketInfo.match} />
                                    </div>
                                    <div className="mb-2">
                                        <label for="where" className="ps-1 h6 mb-2">경기장</label>
                                        <input id="where" className="form-control p-2 ps-3" type="text" readOnly value={ticketInfo.where} />
                                    </div>
                                    <div className="mb-2">
                                        <label for="seat" className="ps-1 h6 mb-2">좌석 정보</label>
                                        <input id="seat" className="form-control p-2 ps-3" type="text" readOnly value={ticketInfo.seat} />
                                    </div>
                                </div>
                            </div>
                        </div>
                    </>}
                    
                    {/* 하단 이미지 첨부 칸 (여러 개 가능) */}
                    <div className={classes.imageSection}>
                        <p className={classes.imageLabel}>직관 사진</p>
                        
                        {/* 이미지 미리보기 그리드 */}
                        {images.length > 0 && (
                            <div className={classes.bottomImagesPreview}>
                                {images.map((imgObj, index) => (
                                    <div key={index} className={classes.bottomImagePreviewItem}>
                                        <img 
                                            src={imgObj.base64}
                                            alt={`직관 사진 ${index + 1}`}
                                            className={classes.bottomImagePreview}
                                        />
                                        <button 
                                            type="button"
                                            className={classes.removeBottomImage}
                                            onClick={() => removeImage(index)}
                                        >
                                            <i className="fas fa-times" />
                                        </button>
                                    </div>
                                ))}
                            </div>
                        )}
                        
                        {/* 이미지 추가 버튼 */}
                        <div className={classes.bottomImageUpload} onClick={() => imagesRef.current?.click()}>
                            <div className={classes.uploadPlaceholder}>
                                <i className="fas fa-plus" />
                                <p>직관 사진 이미지들을 추가하세요.</p>
                            </div>
                        </div>
                        <input
                            ref={imagesRef}
                            type="file"
                            accept="image/*"
                            multiple
                            onChange={handleImagesChange}
                            style={{ display: 'none' }}
                        />
                    </div>

                    {/* 버튼 영역 */}
                    <button className="btn btn-primary p-3 border-radius-0 mt-auto full-width">저장</button>
                </>}
            </form>
            {loading && <LoadingAI content={step === 1 ? "AI가 티켓을 분석중입니다" : "AI가 일기를 생성중입니다"} />}
        </OverlayPage>
    </>
}
