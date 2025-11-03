
export const URL = {
    MAIN: '/',
    SIMULATION: '/simulate',
    HIGHLIGHT: '/highlights',
    PLACE: '/places',
    MEET: '/meets',
    DIARY: '/diary',
    NEWS: '/news',
    MYPAGE: '/my',
    MYINFO: '/my/info',
    MYLOCATION: '/my/location',
    LOGIN: '/login',
    LOGOUT: '/logout',
    REGISTER: '/register',
    NOTIFICATIONS: '/notifications',
    NOTAUTHORIZED: '/not-authorized',
    UNIFORM: '/uniform',
    CHEERSONG: '/cheer-song'
};

export const REGEX = {
    ACCOUNT: "^[a-zA-Z][a-zA-Z0-9_-]{3,19}$",
    PASSWORD: "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,16}$"
};

export const adminmail = 'admin@gmail.com'

export const teams = [
    { id: 'LG', label: 'LG Twins', img: 'LG', home: '잠실야구장' },
    { id: '한화', label: '한화 이글스', img: 'HH', home: '대전한화생명볼파크' },
    { id: '두산', label: '두산 베어스', img: 'OB', home: '잠실야구장' },
    { id: '삼성', label: '삼성 라이온즈', img: 'SS', home: '대구삼성라이온즈파크' },
    { id: 'KIA', label: 'KIA 타이거즈', img: 'HT', home: '광주기아챔피언스필드 ' },
    { id: 'KT', label: 'KT wiz', img: 'KT', home: '수원KT위즈파크' },
    { id: 'NC', label: 'NC 다이노스', img: 'NC', home: '창원NC파크' },
    { id: 'SSG', label: 'SSG 랜더스', img: 'SK', home: '인천SSG랜더스필드' },
    { id: '롯데', label: '롯데 자이언츠', img: 'LT', home: '사직야구장' },
    { id: '키움', label: '키움 히어로즈', img: 'WO', home: '고척스카이돔' }
]

export const teamColors = {
    "LG": "rgba(222, 36, 36, 1)",
    "OB": "rgba(19, 31, 92, 1)",
    "WO": "rgba(141, 37, 37, 1)",
    "SK": "rgba(58, 145, 73, 1)",
    "KT": "rgba(58, 58, 58, 1)",
    "HH": "rgba(255, 109, 36, 1)",
    "HT": "rgba(203, 45, 71, 1)",
    "SS": "rgba(48, 113, 233, 1)",
    "LT": "rgba(55, 44, 172, 1)",
    "NC": "rgba(30, 65, 162, 1)"
};

export default {};