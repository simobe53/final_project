import Empty from '/components/Empty';
import Meet from './components/Meet';

export default function Meets({ meets = [], ref}){
    if (meets.length == 0) return <Empty message="모집글이 없습니다." />
    return<>
        <ul className="flex-grow d-flex flex-column gap-20 p-20 justify-content-start overflow-y-auto">
            {meets.map((meet,idx) => idx != meets.length-1 ?
            (<Meet key={idx} {...meet} />)
            :   
            (<Meet key={idx} {...meet} ref={ref}/>)         
            )}
        </ul>
    </>
}
