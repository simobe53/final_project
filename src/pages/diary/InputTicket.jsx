import { useEffect, useRef, useState } from 'react';
import { Cropper } from 'react-advanced-cropper';
import 'react-advanced-cropper/dist/style.css'
import classes from './Create.module.scss';
import { fileToBase64 } from '/components/File';


export default function InputTicket({ onChange, step, setStep }) {
    const [image, setImage] = useState(null);
    const ticketRef = useRef();
    const cropperRef = useRef(null);

    const defaultSize = ({ imageSize, visibleArea }) => {
        console.log(imageSize);
        console.log(visibleArea);
        return {
            width: (visibleArea || imageSize).width,
            height: (visibleArea || imageSize).height,
        };
    };

	const handleCropper = (cropper) => {
		console.log(cropper.getCoordinates(), cropper.getCanvas());
	};

    const handleImage = (e) => {
        const file = e.target.files[0];
        if (file) {
            fileToBase64(file).then(img => {
                setImage(img);
            })
        }
    };

    const onCrop = (e) => {
        e.preventDefault();
        if (cropperRef.current) {
            setStep(1); // 검증이 시작될 때 step 1로 설정 (실패시 0으로 돌아간다)
            onChange(cropperRef.current.getCanvas()?.toDataURL());
        }
        e.stopPropagation();
    }


    useEffect(() => {
        if (step === 0) setImage(null); // 0으로 스텝이 돌아갈때 이미지 초기화
    }, [step])

	return image ? <>
        <div className="position-relative d-flex align-items-center justify-content-center" style={{ flexGrow: 1, background: '#000' }}>
            <Cropper ref={cropperRef} src={image} onChange={handleCropper} className={'cropper'} defaultSize={defaultSize} />
            <button 
                type="button"
                className={classes.removeImage}
                onClick={(e) => {
                    e.stopPropagation();
                    setImage(null);
                }}
            >
                <i className="fas fa-times"></i>
            </button>
        </div>
        <button className="btn btn-primary p-3 border-radius-0 mt-auto full-width" onClick={onCrop}>티켓 사진으로 사용하기</button>
    </>
	: <>
    <div className={classes.ticketUpload} onClick={() => ticketRef.current?.click()} style={{ borderRadius: 0 }}>
        <div className={classes.uploadPlaceholder}>
            <i className="mt-4 fas fa-camera"></i>
            <p className="h4 mt-3 mb-4" style={{ fontSize: '16px' }}>직관 티켓 사진을 첨부해주세요.</p>
            <p className="pt-4 text-center" style={{ lineHeight: 2 }}>
                ⚠ 주의사항<br/>
                티켓은 QR과 글자가 선명하게 보이도록 첨부해주세요. <br/>
                QR이 존재하지 않으면 직관 일기를 작성하실 수 없습니다. <br/>
                티켓에 적힌 경기를 AI가 감지하여 자동으로 일기를 생성합니다.
            </p>
        </div>
    </div>
    <input
        ref={ticketRef}
        type="file"
        accept="image/*"
        onChange={handleImage}
        style={{ display: 'none' }}
    />
    
    </>
}