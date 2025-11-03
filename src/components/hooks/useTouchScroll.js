import { RefObject, useRef, useEffect, useCallback } from "react";

const useTouchScroll = (containerRef) => {
  const startX = useRef(0);
  const nowX = useRef(0);
  const endX = useRef(0);

  // containerX 변수는 최초 스크롤 시에는 필요 없지만, 요소를 이동한 후의 두 번째 스크롤부터 필요합니다.
  // containerX 변수에는 컨테이너 요소의 translateX 값이 저장되어 있으며, 스크롤 종료 이벤트 함수 안에서 할당합니다.
  // 만약 이 값을 함께 계산하지 않는다면, 요소가 다음과 같이 매 순간 최초 위치(tarnslateX: 0)에서 시작될 것입니다.
  const containerX = useRef(0);

  const containerScrollWidth = useRef(0);
  const containerClientWidth = useRef(0);

  const updateContainerWidths = useCallback(() => {
    containerScrollWidth.current = containerRef.current?.scrollWidth ?? 0;
    containerClientWidth.current = containerRef.current?.clientWidth ?? 0;
  }, [containerRef]);

  // 마우스로 클릭한 지점의 X좌표는 e.clientX로 얻을 수 있지만, 터치 이벤트가 발생한 경우에는 e.touches[0].clientX를 참조해야 합니다.
  const getClientX = useCallback((e) => {
    const isTouches = e.touches && e.touches.length > 0 ? true : false;
    const isChangedTouches = e.changedTouches && e.changedTouches.length > 0 ? true : false;

    if (isTouches) {
      return e.touches[0].clientX;
    } else if (isChangedTouches) {
      return e.changedTouches[0].clientX;
    } else {
      return e.clientX;
    }
  }, []);

  // 스크롤이 종료된 위치도 고려하여 계산해야 하기 때문에 컨테이너 요소의 translateX 위치를 가져와야 합니다.
  // window 객체에 내장된 API인 getComputedStyle 메서드를 사용하면 요소가 가진 CSS의 속성 값을 얻을 수 있는데,
  // transform의 경우 x, y, z의 값을 모두 반환하므로 정규표현식을 통해 필요한 x의 값만 얻도록 했습니다.
  const getTranslateX = useCallback(() => {
    return parseInt(getComputedStyle(containerRef?.current).transform.split(/[^\-0-9]+/g)[5]);
  }, [containerRef]);

  // 스크롤 됨에 따라 요소의 위치를 조정해야 하기 때문에, 간편하게 함수로 만들어 재사용했습니다.
  const setTranslateX = useCallback(
    (x) => {
      containerRef.current.style.transform = `translateX(${x}px)`;
    },
    [containerRef]
  );

  // 스크롤 중에도 계속해서 현재 마우스 포인터가 위치하는 지점에 대한 X 좌표를 nowX 변수에 저장하면서,
  // 시작 지점이 저장된 nowX와의 값의 차를 요소의 translateX 속성 값으로 사용합니다.
  const onScrollMove = useCallback(
    (e) => {
      nowX.current = getClientX(e);
      setTranslateX(containerX.current + nowX.current - startX.current);
    },
    [getClientX, setTranslateX]
  );

  // 컨테이너 요소가 정해진 범위를 벗어나면 보정해 주고, 모든 이벤트를 제거하는 역할을 담당합니다.
  // 범위를 벗어나면 유효 범위로 자연스럽게 돌아올 수 있도록 애니메이션을 부여했습니다.
  // 그리고 애니메이션이 300ms 동안 지속되기 때문에, 제거한 이벤트를 300ms 이후 다시 바인딩할 수 있도록 setTimeout 함수를 사용했습니다.
  const onScrollEnd = useCallback(
    (e) => {
      endX.current = getClientX(e);
      containerX.current = getTranslateX();
      containerScrollWidth.current = containerRef.current?.scrollWidth || 0;
      containerClientWidth.current = containerRef.current.parentNode.clientWidth || 0;

      if (containerX.current > 0) {
        setTranslateX(0);
        containerRef.current.style.transition = `all 0.3s ease`;
        containerX.current = 0;
      } else if (containerX.current < containerClientWidth.current - containerScrollWidth.current) {
        setTranslateX(containerClientWidth.current - containerScrollWidth.current);
        containerRef.current.style.transition = `all 0.3s ease`;
        containerX.current = containerClientWidth.current - containerScrollWidth.current;
      }

      window.removeEventListener("mousemove", onScrollMove);
      window.removeEventListener("touchmove", onScrollMove);
      window.removeEventListener("mouseup", onScrollEnd);
      window.removeEventListener("touchend", onScrollEnd);

      setTimeout(() => {
        if (containerRef.current) containerRef.current.style.transition = "";
      }, 300);
    },
    [getClientX, getTranslateX, setTranslateX, containerRef, onScrollMove]
  );

  // 스크롤을 시작하게 되면 마우스 또는 터치한 지점을 startX 변수에 저장하고, 나머지 이벤트를 마저 등록합니다.
  // 하지만 마우스를 떼더라도 스크롤링이 이어질 텐데, 이는 마지막 스크롤 종료 이벤트에서 처리하면 됩니다.
  const onScrollStart = useCallback(
    (e) => {
      startX.current = getClientX(e);
      containerX.current = getTranslateX();
      window.addEventListener("mousemove", onScrollMove);
      window.addEventListener("touchmove", onScrollMove);
      window.addEventListener("mouseup", onScrollEnd);
      window.addEventListener("touchend", onScrollEnd);
    },
    [getClientX, onScrollMove, onScrollEnd, getTranslateX]
  );

  // 마우스나 터치 이벤트가 발생한 후에는 클릭 이벤트가 추가로 발생합니다.
  // 그렇기 때문에 스크롤링을 하지 않은 경우에만 클릭 이벤트가 발생하도록 처리해야 합니다.
  const onClick = useCallback((e) => {
    if (startX.current - endX.current !== 0) {
      e.preventDefault();
    }
  }, []);

  // 가장 처음 실행되어야 하는 이벤트 등록
  useEffect(() => {
    const currentContainerRef = containerRef.current;
    const isMobile = /iphone|ipad|ipod|android/i.test(navigator.userAgent.toLowerCase());

    // 모바일일 경우에는 transform 애니메이션을 통해서 스크롤 하지 않고 일반적인 스크롤로 구현되도록 설정합니다. (CSS도 마찬가지)
    if (currentContainerRef && !isMobile) {
      currentContainerRef.addEventListener("mousedown", onScrollStart);
      currentContainerRef.addEventListener("touchstart", onScrollStart);
      currentContainerRef.addEventListener("click", onClick);
    }

    return () => {
      if (currentContainerRef) {
        currentContainerRef.removeEventListener("mousedown", onScrollStart);
        currentContainerRef.removeEventListener("touchstart", onScrollStart);
        currentContainerRef.removeEventListener("click", onClick);
      }
      window.removeEventListener("mousemove", onScrollMove);
      window.removeEventListener("touchmove", onScrollMove);
      window.removeEventListener("mouseup", onScrollEnd);
      window.removeEventListener("touchend", onScrollEnd);
    };
  }, [containerRef, onScrollStart, onClick, onScrollMove, onScrollEnd]);

  // 윈도우 리사이즈 이벤트 핸들러 등록
  useEffect(() => {
    updateContainerWidths();
    window.addEventListener("resize", updateContainerWidths);

    return () => {
      window.removeEventListener("resize", updateContainerWidths);
    };
  }, [updateContainerWidths]);

  return { setTranslateX };
};

export default useTouchScroll;
