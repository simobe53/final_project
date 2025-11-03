import { useEffect, useState } from 'react';
import classes from './Input.module.scss';
import { REGEX } from '/config/constants';

export default function Input({
    id, placeholder, label, required, errorMessage, ...props
}) {
    const [error, setError] = useState("");
    const [shrink, setShrink] = useState(false);
    const regex = props.name && REGEX[props.name?.toUpperCase()] || '';
    const { value: inputValue = null } = props;
    const valueExists = !!inputValue;

    useEffect(() => {
        if (errorMessage) setError(`* ${errorMessage}`);
    }, [errorMessage]);

    useEffect(() => setShrink(valueExists), [valueExists]);

    const handleBlur = e => {
        if (!required) return;
        const { value } = e.target;
        if (!value) {
            setError("* 이 값은 필수입니다!");
        } 
        else if (regex) {

            if (!new RegExp(regex).test(value)) {
                let err;
                switch (regex) {
                    case REGEX.ACCOUNT : err = "영문소문자로 시작하고 영문소문자/숫자/_-만 허용 (4~20자)"; break;
                    case REGEX.PASSWORD : err = "대소문자, 숫자, 특수문자 포함 (8~16자)"; break;
                    default:
                }
                setError(err);
            } else {
                setError(null);
            }
        }
        else setError(errorMessage || "");
    }

    useEffect(() => setError(""), [inputValue])

    return <>
        <div className={`${classes.field} ${error ? classes.error : ''}`}>
            <input id={id} className={`form-control ${required ? classes.required : ''}`} onFocus={() => setShrink(true)} onBlur={handleBlur} {...props} />
            <label htmlFor={id} className={shrink ? classes.shrink : ''}><small>{label}</small><span>{placeholder || label+'를 입력하세요'}</span></label>
            {error && <small className={classes.errorMessage}>{error}</small>}
        </div>
    </>
}