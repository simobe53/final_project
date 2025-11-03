export default function ProfileImg({ src, small, zoom = 1, ...props }) {
    return <>
        <div className={`profile-card__image user border border-gray ${small ? 'small' : ''}`} style={{ zoom }}>
            {src && <img src={src} alt="" {...props} />}
        </div>
    </>
}
