import scss from './Loading.module.scss';

//  <<<     데이터 뿌려주기 전 로딩 UI 컴포넌트   >>>
export default function Loading () {
    return <>
        <div className="d-flex align-items-center justify-content-center" style={{ height: '100%' }}>
            <span className={scss.loader}></span>
        </div>
    </>
}
