import scss from './LoadingAI.module.scss';

//  <<<     데이터 뿌려주기 전 로딩 UI 컴포넌트   >>>
export default function LoadingAI({ content }) {
    return <>
        <div className={`d-flex flex-column align-items-center justify-content-center gap-20 ${scss.container}`}>
            <span className={scss.loader}></span>
            {content && <p className="h5 text-white">{content}</p>}
        </div>
    </>
}
