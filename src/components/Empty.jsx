export default function Empty({ message = '컨텐츠가 없습니다.' }) {
    return <>
        <div className="d-flex align-items-center full-height justify-content-center flex-column">
            <div className="m-auto d-flex flex-column gap-20 align-items-center">
                <img src="/assets/icons/empty.png" style={{ opacity: 0.3, marginTop: -20 }} />
                <span style={{ fontSize: '1.1rem', opacity: 0.6 }}>{message}</span>
            </div>
        </div>
    </>
}