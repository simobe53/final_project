import { Link } from 'react-router-dom';
import { URL } from '/config/constants';
import classes from './Logo.module.scss';

export default function Logo(props) {
    return <>
        <span className={classes.logo} {...props}>
            <Link to={URL.MAIN}>
                <img src="/assets/icons/logo.gif" alt="logo" />
            </Link>
        </span>
    </>
}
