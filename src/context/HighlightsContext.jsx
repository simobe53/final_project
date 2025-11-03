/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useState } from "react";

const dummyHighlights = [
    {"masterVid":"D5E73B5D3E38B2020939DA38E0676EB44C12","clipNo":"82720756","title":"신입 마법사 김동현의 첫 1군 데뷔 소감｜(08.21) ","thumbnail":"https://phinf.pstatic.net/tvcast/20250822_148/Bjd2M_1755829978039htieV_JPEG/OaL7L7XATu_01.jpg","videoType":"6","videoTypeName":"인터뷰","seasonCode":"kboteam04","seasonName":"kt wiz","divisionCode":"9111","divisionName":"구단특집","upperCategoryId":"kbaseball","categoryId":"kbo","playTime":"00:02:25","hit":65,"produceDateTime":"2025-08-22T12:00:00","sportsVideoId":1367374},
    {"masterVid":"94326A52996DF0772D95A210E9ABFE632FCB","clipNo":"82448830","title":"우린 원팀 왕년에 공 좀 던져본 박기량 & 두치 새내기 아야카의 시구시타 현장","thumbnail":"https://phinf.pstatic.net/tvcast/20250818_82/47lLb_17554802499111KNvq_PNG/image.png","videoType":"7","videoTypeName":"핫클립","seasonCode":"kboteam05","seasonName":"두산베어스","divisionCode":"9111","divisionName":"구단특집","upperCategoryId":"kbaseball","categoryId":"kbo","playTime":"00:06:03","hit":370,"produceDateTime":"2025-08-20T17:43:11","sportsVideoId":1366634},
    {"masterVid":"D9914D9E0F7600243E6C20D7D30B15428276","clipNo":"82576968","title":"오늘도 해냈다 9회말 끝내기로 만든 2연승 I vs SSG (08.19) [위즈덕후]","thumbnail":"https://phinf.pstatic.net/tvcast/20250820_263/Fr21m_1755651681718g18JO_JPEG/image.jpg","videoType":"7","videoTypeName":"핫클립","seasonCode":"kboteam04","seasonName":"kt wiz","divisionCode":"9111","divisionName":"구단특집","upperCategoryId":"kbaseball","categoryId":"kbo","playTime":"00:17:54","hit":410,"produceDateTime":"2025-08-20T10:10:00","sportsVideoId":1366491},
    {"masterVid":"7FAA16245380A513AAC57F0CCCD89C1657BB","clipNo":"82568094","title":"얘들아, 우리 승리 주역들이 인터뷰한대! 다들 댓글 예쁘게 달아(주세요) [콕터뷰]","thumbnail":"https://phinf.pstatic.net/tvcast/20250820_162/KR5Iz_1755631553287DSbzB_JPEG/image.jpg","videoType":"6","videoTypeName":"인터뷰","seasonCode":"kboteam04","seasonName":"kt wiz","divisionCode":"9111","divisionName":"구단특집","upperCategoryId":"kbaseball","categoryId":"kbo","playTime":"00:09:01","hit":331,"produceDateTime":"2025-08-20T04:25:56","sportsVideoId":1366413},
    {"masterVid":"E79D45BB9E2F0557A9B771C334288D440D24","clipNo":"82554102","title":"끝내기 단골 손님 입장이요!!! [위즈덕후]","thumbnail":"https://phinf.pstatic.net/tvcast/20250819_32/ye896_1755610722664V7Vsh_JPEG/image.jpg","videoType":"7","videoTypeName":"핫클립","seasonCode":"kboteam04","seasonName":"kt wiz","divisionCode":"9111","divisionName":"구단특집","upperCategoryId":"kbaseball","categoryId":"kbo","playTime":"00:02:15","hit":319,"produceDateTime":"2025-08-19T22:40:05","sportsVideoId":1366412},
    {"masterVid":"85129D18B117EEAAA7E0B29522AFDFBDC693","clipNo":"82286431","title":"[히어로캠] 천천히 하나하나 집중해서 만들어낸 0봉승알칸타라 7이닝 무실점 4K + 조영건 데뷔 첫 세이브 & 송성문 결승타","thumbnail":"https://phinf.pstatic.net/tvcast/20250815_42/5Y91t_1755208902244Kcvqy_JPEG/image.jpg","videoType":"7","videoTypeName":"핫클립","seasonCode":"kboteam07","seasonName":"키움히어로즈","divisionCode":"9111","divisionName":"구단특집","upperCategoryId":"kbaseball","categoryId":"kbo","playTime":"00:14:44","hit":331,"produceDateTime":"2025-08-15T07:12:36","sportsVideoId":1364917},
    {"masterVid":"19D2C89B4A1427981D3A9B09557FB349DE0B","clipNo":"82281619","title":"[큠TALK] 마무리가 원래 이렇게 떨리는 거예요?","thumbnail":"https://phinf.pstatic.net/tvcast/20250815_115/WZyLo_17551903285420IMJ0_JPEG/image.jpg","videoType":"6","videoTypeName":"인터뷰","seasonCode":"kboteam07","seasonName":"키움히어로즈","divisionCode":"9111","divisionName":"구단특집","upperCategoryId":"kbaseball","categoryId":"kbo","playTime":"00:02:17","hit":205,"produceDateTime":"2025-08-15T01:52:41","sportsVideoId":1364916},
    {"masterVid":"25901D9A65CA589271B5C2C32AAF4A5521DA","clipNo":"82183433","title":"커브볼로 스트존 꽂아 넣는 걸그룹 실존! 아르테미스 희진&최리의 시구·시타 현장 [#두케터]","thumbnail":"https://phinf.pstatic.net/tvcast/20250813_81/kFEUu_1755073018288Fn9nE_PNG/image.png","videoType":"7","videoTypeName":"핫클립","seasonCode":"kboteam05","seasonName":"두산베어스","divisionCode":"9111","divisionName":"구단특집","upperCategoryId":"kbaseball","categoryId":"kbo","playTime":"00:03:55","hit":352,"produceDateTime":"2025-08-13T17:03:00","sportsVideoId":1364377}
]

const HighlightsContext = createContext(null);

export function HighlightsProvider({ children }) {
    const [model, setModel] = useState([]);  // 전역 state;

    useEffect(() => {
        /** [TODO] :: get data from api */
        setModel(dummyHighlights)
    }, [])

    return <>
        <HighlightsContext.Provider value={{ model }}>
            {children}
        </HighlightsContext.Provider>
    </>
}

export function useHighlights() {
    return useContext(HighlightsContext);
}
