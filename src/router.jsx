import { createBrowserRouter } from "react-router-dom";
import { URL } from "/config/constants";
import { InitProvider } from "/context/InitContext";
import { AuthProvider } from "/context/AuthContext";
import { HighlightsProvider } from "/context/HighlightsContext";
import { SimulationsProvider } from "/context/SimulationsContext";
import { DiaryProvider } from "/context/DiaryContext";
import NotFound from "/pages/NotFound";
import Home from "/pages/Home";
import NewsHighlights from "/pages/news";
import NewsDetail from "/pages/news/NewsDetail";
import HighlightsDetail from "/pages/news/HighlightsDetail";
import SimulateGames from "/pages/simulgames";
import SimulateGamesDetail from "/pages/simulgames/Detail";
import CreateSimulation from "/pages/simulgames/Create";
import MyPage from "/pages/mypage";
import MyMeet from "/pages/mypage/MyMeet";
import MyPlace from "/pages/mypage/MyPlace";
import MyScrapPlace from "/pages/mypage/MyScrapPlace";
import MyReview from "/pages/mypage/MyReview";
import MyPageUserInfo from "/pages/mypage/UserInfo";
import Login from "/pages/login/Login";
import Signup from "/pages/login/Signup";
import Place from "/pages/place";
import CreatePlace from "/pages/place/Create";
import DetailPlace from "/pages/place/Detail";
import CreatePlaceReview from "/pages/place/CreateReview";
import Meet from "/pages/meet";
import CreateMeet from "/pages/meet/Create";
import DetailMeet from "/pages/meet/Detail";
import ModifyMeet from "/pages/meet/Modify";
import Diary from "/pages/diary";
import CreateDiary from "/pages/diary/Create";
import MyUniform from "/pages/mypage/MyUniform";
import NotAuthorized from "/pages/NotAuthorized";
import MyCheerSong from "/pages/mypage/MyCheerSong";
import PaymentComplete from "/pages/payment/Complete";
import App from "./App";
import AuthWithReset from "./AuthWithReset";


const router = createBrowserRouter([
    {
        path: '/',
        element: <>
            <InitProvider>
                <AuthWithReset>
                    <App />
                </AuthWithReset>
            </InitProvider>
        </>,
        children: [
            { path: URL.LOGIN, element: <Login /> },
            { path: URL.REGISTER, element: <Signup /> },

            { path: "", element: <HighlightsProvider><Home /></HighlightsProvider> },
            { 
                path: `${URL.SIMULATION}`, element: <SimulationsProvider><SimulateGames /></SimulationsProvider>,
                children: [
                    { path: `create`, element: <CreateSimulation /> },
                    { path: `:id`, element: <SimulateGamesDetail /> },
                ]
            },
            { 
                path: `${URL.PLACE}`,
                element: <Place />,
                children: [
                    { path: `create`, element: <CreatePlace /> },
                    { path: ':id', element: <DetailPlace /> },
                    { path: `:id/createReview`, element: <CreatePlaceReview /> },
                ]
            },

            { 
                path: URL.MEET,
                element: <Meet />,
                children: [
                    { path: `create`, element: <CreateMeet /> },
                    { path: `modify/:id`, element: <ModifyMeet />},
                    { path: ':id', element: <DetailMeet /> },
                ]
            },

            { 
                path: URL.DIARY,
                element: <DiaryProvider><Diary /></DiaryProvider>,
                children: [
                    { path: `create`, element: <CreateDiary /> }
                ]
            },

            { 
                path: URL.NEWS, element: <NewsHighlights />,
                children: [
                    { path: `${URL.NEWS}/:id`, element: <NewsDetail /> },
                    { path: `${URL.NEWS}/${URL.HIGHLIGHT}/:id`, element: <HighlightsProvider><HighlightsDetail /></HighlightsProvider> },
                ]
            },
            
            { path: URL.NOTAUTHORIZED, element: <NotAuthorized /> },
            { path: URL.MYPAGE, element: <MyPage /> },
            { path: `${URL.MYPAGE}${URL.CHEERSONG}`, element: <MyCheerSong /> },
            { path: `${URL.MYPAGE}${URL.MEET}`, element: <MyMeet/> },
            { path: `${URL.MYPAGE}${URL.PLACE}`, element: <MyPlace/> },
            { path: `${URL.MYPAGE}${URL.PLACE}/scrap`, element: <MyScrapPlace/> },
            { path: `${URL.MYPAGE}${URL.PLACE}/review`, element: <MyReview/> },
            { path: `${URL.MYPAGE}${URL.UNIFORM}`, element: <MyUniform /> },
            { path: URL.MYINFO, element: <MyPageUserInfo /> },

            { path: '/payment/complete', element: <PaymentComplete /> },

            { path: "*", element: <NotFound /> }
        ]
    }
]);

export default router;