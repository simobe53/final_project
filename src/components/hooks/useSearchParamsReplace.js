import { useSearchParams, useNavigate } from 'react-router-dom';

/**
 * useSearchParamsReplace
 * - 기존 useSearchParams와 거의 동일하게 사용 가능
 * - setSearchParams 시 브라우저 히스토리를 replace
 * - 기존 쿼리 파라미터는 유지
 */
export function useSearchParamsReplace() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const setSearchParamsReplace = (params) => {
    // 기존 searchParams 복사
    const newParams = new URLSearchParams(searchParams);

    // 전달된 params를 적용
    Object.entries(params).forEach(([key, value]) => {
      if (value === null || value === undefined) {
        newParams.delete(key); // 값이 null/undefined이면 삭제
      } else {
        newParams.set(key, value);
      }
    });

    // replace 옵션으로 URL 변경
    navigate({ search: newParams.toString() }, { replace: true });
  };

  return [searchParams, setSearchParamsReplace];
}
