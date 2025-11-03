import classes from './Search.module.scss';

export default function Search({ onSearch, ...inputProps }) {

    return <>
        <div className={classes.search}>
            <input type="text" {...inputProps} />
            <button onClick={onSearch}>
                <i className="fa-solid fa-magnifying-glass"></i>
            </button>
        </div>
    </>
}