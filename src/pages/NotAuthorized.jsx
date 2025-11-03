import classes from './NotFound.module.scss';
import { adminmail } from '/config/constants';

export default function NotAuthorized() {

    return <>
        <div className={classes.noneService}>
            <div className={classes.inner}>
                <img src="/assets/icons/ico_none.svg" alt="" />
                <h2>
                    <b className="point">권한이 없는 페이지</b><strong>입니다.</strong>
                </h2>
                <p style={{ lineHeight: 1.5 }}>
                    이용하시려면 로그인 해주세요.<br />
                    로그인 상태로도 이 페이지가 뜬다면 관리자에게 문의하세요. <br/>
                    <br/>
                    <a href={`mailto:${adminmail}`} className="btn-link">관리자에게 문의하기</a>
                </p>
            </div>
        </div>
    </>
}