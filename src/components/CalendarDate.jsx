import { useEffect, useMemo, useRef, useState } from 'react';
import useTouchScroll from './hooks/useTouchScroll';
import classes from './Calendar.module.scss';

// ✅ 올바른 윤년 계산 (KST용)
const getEndDate = (month, year) => {
  if ([4, 6, 9, 11].includes(month)) return 30;
  if (month === 2)
    return (year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0)) ? 29 : 28;
  return 31;
};

export default function CalendarDate({ date: prevDate, onChange }) {
  const [x, setX] = useState(0);
  const containerRef = useRef();
  const { setTranslateX } = useTouchScroll(containerRef);
  const [date, setDate] = useState(prevDate);

  const year = useMemo(() => date.getFullYear(), [date]);
  const month = useMemo(() => date.getMonth(), [date]);
  const dt = useMemo(() => date.getDate(), [date]);

  const dates = useMemo(() => {
    const ret = [];
    for (let i = 1; i <= getEndDate(month + 1, year); i++) ret.push(i);
    return ret;
  }, [month, year]);

  const changeDate = (d, e) => {
    e.preventDefault();
    const newDate = new Date(year, month, d); // ✅ KST 기준
    setDate(newDate);
    e.stopPropagation();
  };

  useEffect(() => {
    if (prevDate.getTime() !== date.getTime()) setDate(prevDate);
  }, [prevDate]);

  useEffect(() => {
    if (onChange) onChange(date);
  }, [date, onChange]);

  useEffect(() => {
    if (dt >= 22) setX(-476);
    else if (dt <= 10) setX(0);
    else setX(-(dt - 10) * 42);
  }, [dt]);

  return (
    <div className={classes.rootDate}>
      <div
        ref={containerRef}
        className="d-flex"
        onDrag={setTranslateX}
        style={{ transform: `translateX(${x}px)` }}
      >
        {dates.map((d) => (
          <button
            key={d}
            className={`btn btn-none h4 mb-0 ${dt === d ? classes.active : ''}`}
            onClick={(e) => changeDate(d, e)}
          >
            {d}
          </button>
        ))}
      </div>
    </div>
  );
}
