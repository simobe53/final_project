import { NavLink } from 'react-router-dom';
import { URL } from '/config/constants';
import classes from './Menu.module.scss';

const menus = [
  { path: URL.MAIN, label: '홈', icon: 'baseball.png' },
  { path: URL.SIMULATION, label: '시뮬레이션', icon: 'simulation.png' },
  { path: URL.PLACE, label: '팀별맛집지도', icon: 'food.png' },
  { path: URL.MEET, label: '직관 모집', icon: 'stadium.png' },
  { path: URL.DIARY, label: '직관 일기', icon: 'diary.png' },
  { path: URL.NEWS, label: '뉴스', icon: 'softball.png' },
  { path: URL.MYPAGE, label: '더보기', icon: 'more.png' }
]

function Menu() {
  return (
    <nav className={classes.menu}>
      {menus.map(({ path, label, icon }) => (
        <NavLink key={label} to={path} className={({ isActive }) => `${classes.menu_item} h6 ${isActive ? classes.active : ''}`}>
          <img src={`/assets/icons/${icon}`} width="auto" height="36%" alt="" />
          {label}
        </NavLink>
      ))}
    </nav>
  )
}

export default Menu;

