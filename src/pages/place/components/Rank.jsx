import { useEffect, useState } from "react";
import { Rating } from 'react-simple-star-rating'

export function Rank({ readOnly, value = 0, size = 25, onChange }) {
    const [rank, setRank] = useState(value);

    useEffect(() => {
        if (value !== rank) setRank(value);
    }, [value]);

    return <>
        <input type="number" name="rank" className="invisible position-absolute" value={rank} readOnly />
        <Rating onClick={onChange || setRank} readonly={readOnly} initialValue={rank} size={size} />
    </>
}