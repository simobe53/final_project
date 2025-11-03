import { useEffect, useState } from 'react';
import classes from './Calendar.module.scss';
import { formatDate } from '.'; // KST 기준으로 동작
import CalendarDate from './CalendarDate';
import { useSearchParamsReplace } from './hooks/useSearchParamsReplace';

// ✅ "YYYY-MM-DD" → Date (KST 기준)
const parseLocal = (str) => {
  const [y, m, d] = str.split('-').map(Number);
  return new Date(y, m - 1, d); // 로컬 타임존 기준
};

export default function Calendar({ defaultValue, onChange }) {
  const [searchParams, setSearchParamsReplace] = useSearchParamsReplace();
  const dateparam = searchParams.get('date');
  const [date, setDate] = useState(defaultValue || new Date());

  const setLastMonth = () => {
    const dt = new Date(date);
    dt.setMonth(dt.getMonth() - 1);
    setDate(dt);
  };

  const setNextMonth = () => {
    const dt = new Date(date);
    dt.setMonth(dt.getMonth() + 1);
    setDate(dt);
  };

  useEffect(() => {
    if (dateparam) setDate(parseLocal(dateparam));
    else if (defaultValue) setDate(defaultValue);
  }, [dateparam, defaultValue]);

  useEffect(() => {
    if (onChange) {
      const dt = formatDate(date); // "YYYY-MM-DD", 로컬(KST) 기준
      onChange(dt);
      setSearchParamsReplace({ date: dt });
    }
  }, [date, onChange]);

  return (
    <>
      <div className={classes.root}>
        <button className="btn border border-radius-20" onClick={setLastMonth}>
          <i className="fas fa-angle-left" />
        </button>
        <div>
          <i className="fas fa-calendar-check me-1 point" style={{ fontSize: 20 }} />
          <label htmlFor="date-picker">
            <input
              type="date"
              id="date-picker"
              value={formatDate(date)}
              onChange={(e) => setDate(parseLocal(e.target.value))}
            />
          </label>
        </div>
        <button className="btn border border-radius-20" onClick={setNextMonth}>
          <i className="fas fa-angle-right" />
        </button>
      </div>
      <CalendarDate date={date} onChange={setDate} />
    </>
  );
}
