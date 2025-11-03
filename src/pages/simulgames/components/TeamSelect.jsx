import TeamLogo from "/components/TeamLogo";

export default function TeamSelect({ team, selected, onSelect }) {
    if (!team) return null;

    return <>
        <button
            type="button"
            className={`btn border-radius-12 p-3 d-flex flex-column align-items-center gap-2 ${
                selected ? 'btn-primary' : 'btn-outline-secondary'
            }`}
            style={{ minWidth: 100 }}
            onClick={onSelect}
        >
            <TeamLogo name={team.id} zoom={0.8} />
            <small className="text-nowrap">{team.label}</small>
        </button>
    </>
}
