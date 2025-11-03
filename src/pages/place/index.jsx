import { useEffect, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useInit } from "/context/InitContext";
import { useAuth } from "/context/AuthContext";
import { getPlaces } from "/services/places";
import useTouchScroll from "/components/hooks/useTouchScroll";
import Places from "./Places";

export default function PlacesContainer(props) {
    const { auth: { team: { id: userTeamId } = {} } = {} } = useAuth();
    const [places, setPlaces] = useState([]);
    const { teams } = useInit();
    const [categories, setCategories] = useState([]);
    const [selected, setSelected] = useState(userTeamId || 1);
    const containerRef = useRef();
    const { setTranslateX } = useTouchScroll(containerRef);
    const [params] = useSearchParams();

    const category = params.get('category');

    useEffect(() => {
        /* TODO:: 여기서 리스트 불러와서 저장하기 */
        getPlaces(selected).then(data => {
            setPlaces(data);
            const cate = [];
            data.forEach(({ category }) => { if(!cate.includes(category)) cate.push(category) });
            setCategories(cate);
        })
    }, [selected]);

    useEffect(() => {
        if (userTeamId !== selected) setSelected(userTeamId || 1)
    }, [userTeamId])

    const filteredPlaces = !selected ? places : places.filter(place => place.team?.id === selected && (!category || place.category === category));

    console.log(selected);
    return <>
        <div className="position-absolute overflow-hidden" style={{ width: '100%', top: 60, left: 0, zIndex:2 }}>
            <div ref={containerRef} className="d-flex gap-8 p-2" onDrag={setTranslateX} style={{ transform: `translateX(0px)` }}>
                {teams.map(team => <button key={team.id} 
                    className={`p-0 ps-1 pe-2 btn text-nowrap btn-sm category-btn border-radius-20 ${selected === team.id ? "selected" : ""}`} 
                    onClick={() => setSelected(selected === team.id ? null : team.id)}
                >
                    <span className="d-flex gap-1 align-items-center">
                        <img src={`/assets/icons/${team.idKey}.png`} width="32px" />
                        {team.name}
                    </span>
                </button>)}
            </div>
        </div>
        <Places {...props} places={filteredPlaces} setPlaces={setPlaces} />
    </>
}