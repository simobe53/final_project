export default function TeamSelect({ selected, team, setTeam, isSelected, onClick, disabled }) {
    const { id, idKey, name } = team;
    const onChange = () => {
        if (disabled) return;
        if (onClick) onClick();
        else setTeam(selected?.id === team?.id ? {} : team)
    };

    return <>
        <div 
            id={idKey} 
            className={`border-radius-12 d-flex flex-column align-items-center ${disabled ? "" : "pointer"}`}
            style={{ width: 172, height: 180, border: isSelected || (selected?.id === id) ? '3px solid var(--point-color)' : '1px solid var(--gray-border-color)' }} 
            onClick={onChange}
        >
            <div className="m-auto text-center">
                <img src={`/assets/icons/${idKey}.png`} alt="" style={{ width: 132 }} />
                <p>{name}</p>
            </div>
        </div>
    </>
}
