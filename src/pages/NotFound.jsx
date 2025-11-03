import classes from './NotFound.module.scss';

export default function NotFound() {

    return <>
        <div className={classes.noneService}>
            <div className={classes.inner}>
                <img src="/assets/icons/ico_none.svg" alt="" />
                <h2>
                    <b className="point">서비스 준비중</b><strong>입니다.</strong>
                </h2>
                <p style={{ lineHeight: 1.5 }}>
                    보다 나은 서비스 제공을 위해 페이지 준비중에 있습니다.<br />
                    홈페이지를 방문해 주셔서 감사합니다.
                </p>
            </div>
        </div>
    </>
}