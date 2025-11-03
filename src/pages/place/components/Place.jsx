import { Link } from 'react-router-dom';
import { URL } from '/config/constants';
import ScrapBtn from './ScrapBtn';

export default function Place({ id, image, name, category, address, showScrap }) {
    return <>
        <li className="d-flex flex-column" style={{ flex: 1, width: 216 }}>
            <Link to={`${URL.PLACE}/${id}`} className="image-square position-relative" style={{ backgroundImage: `url('${image}')` }}>
                {showScrap && <>
                    <div className="position-absolute" style={{ right: 8, bottom: 8 }}>
                        <ScrapBtn placeId={id} />
                    </div>
                </>}
            </Link>
            <p className="ps-1 pt-1 text-nowrap overflow-hidden text-truncate">{name}</p>
            <small className="ps-1 text-gray">{category}</small>
            <p className="ps-1 text-gray small text-nowrap overflow-hidden text-truncate">{address}</p>
        </li>
    </>
}