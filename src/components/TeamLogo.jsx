
const fileName = {
    '롯데': 'LT',
    '한화': 'HH',
    '삼성': 'SS',
    'SSG': 'SK',
    '키움': 'WO',
    '두산': 'OB',
    'KIA': 'HT'
};

export default function TeamLogo({ name, small, zoom = 1, ...props }) {
    return <>
        <span className={`profile-card__image border border-gray ${small ? 'small' : ''}`} style={{ zoom }}>
            {name && <img src={`/assets/icons/${fileName[name] || name}.png`} alt="" {...props} />}
        </span>
    </>
}
