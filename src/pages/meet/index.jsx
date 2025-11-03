import { useEffect, useRef, useState } from "react";
import { Link, Outlet } from 'react-router-dom';
import { useInView } from "react-intersection-observer";
import { URL } from '/config/constants';
import { useAuth } from "/context/AuthContext";
import { getMeetAllWithPage } from "/services/meets";
import Schedules from "/pages/schedules/Schedules";
import Meets from "./Meets";


export default function MeetsContainer(props) {
    const { auth } = useAuth();
    const [date, setDate] = useState(null);
    const [meets, setMeets] = useState([]);
    const [renew , setRenew] = useState(false);
    // 전체 게시글 저장용
    const [ref,inView] = useInView();
    const [loading,setLoading] = useState(false);
    const [page,setPage] = useState(1);
    const prevRenewRef = useRef(renew);
    const prevPageRef = useRef(page);
    
    useEffect(() => {
        const prevRenew = prevRenewRef.current;
        const prevPage = prevPageRef.current;

        const isRenewChanged = renew !== prevRenew;
        const isPageChanged = page !== prevPage;
        if(!isPageChanged&&!isRenewChanged){
            getMeetAllWithPage(page)
            .then(data => {
            if (data) {
                setMeets(prev=> [...prev,...data]);
            }
            }).catch(err=>console.log(err.response.data.ERROR))
        }
        else if(isPageChanged&&page != 1 ) {
            console.log("isPageChanged")
            getMeetAllWithPage(page)
            .then(data => {
                if (data) {
                    setMeets(prev=> [...prev,...data]);
                }
            }).catch(err=>console.log(err.response.data.ERROR))
        }
        else if(isRenewChanged ){
            console.log("isRenewChanged")
            getMeetAllWithPage(1)
            .then(data => {
            if (data) {
                setMeets(data);
                setPage(1);
            }
        }).catch(err=>console.log(err.response.data.ERROR))
        }
        prevRenewRef.current = renew;
        prevPageRef.current = page;
    }, [renew,page]);

    
    useEffect(()=>{
        //inView = true , 즉 마지막 요소가 보이는 상태이면
        if(inView){
            console.log("인뷰데이터")
            setLoading(true)
            setPage(prev=>prev+1)
        }
    },[inView])

    return <>
        <div className="d-flex flex-column" style={{ minHeight: '100%', height: '100%' }}>
            <div className="border-bottom bg-white border-gray position-sticky bg-white p-3 pb-2" style={{ top: 0, zIndex: 1 }}>
                <p className="h6">예정된 경기</p>
                <div>
                    <Schedules />
                </div>
            </div>
            <Meets {...props} meets={meets} ref={ref} />
        </div>
        {!!auth.id && <Link to={`${URL.MEET}/create`} className="create_button" />}
        <Outlet context={{ setMeets, setRenew }} /> 
    </>
}