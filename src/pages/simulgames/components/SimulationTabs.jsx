import { useState } from "react";
import Relay from "./Relay";
import Article from "./Article";
import classes from "./SimulationTabs.module.scss";

export default function SimulationTabs({ simulationId, atBats, homeTeam, awayTeam }) {
    const tabs = [
        { id: "relay", label: "중계" },
        { id: "article", label: "뉴스" },
    ];

    const [activeTab, setActiveTab] = useState("relay");

    return (
        <div className={classes.gameTab}>
            <ul className={classes.tabList} role="tablist">
                {tabs.map((tab) => (
                    <li key={tab.id} className={classes.tabItem} role="presentation">
                        <button
                            type="button"
                            role="tab"
                            className={`${classes.tabButton} ${activeTab === tab.id ? classes.active : ""}`}
                            aria-selected={activeTab === tab.id}
                            onClick={() => setActiveTab(tab.id)}
                        >
                            <span className={classes.tabText}>{tab.label}</span>
                        </button>
                    </li>
                ))}
            </ul>

            <div className={classes.tabContent}>
                {activeTab === "relay" && <Relay atBats={atBats} home={homeTeam} away={awayTeam} />}
                {activeTab === "article" && <Article simulationId={simulationId} />}
            </div>
        </div>
    );
}
