/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useState } from "react";

const DiaryContext = createContext(null);

export function DiaryProvider({ children }) {
    const [model, setModel] = useState(() => {});

    useEffect(() => {
        // TODO:: get api 호출 후 저장. [key(날짜)] : {} 로 저장한다.
    }, [])

    const fetchModel = (data) => {
        setModel(prev => ({ ...prev, [data.createdAt]: data }));
    }

    return <>
        <DiaryContext.Provider value={{ model, fetchModel }}>
            {children}
        </DiaryContext.Provider>
    </>
}

export function useDiary() {
    return useContext(DiaryContext);
}
