import { useRef, useEffect } from 'react';

export default function usePasswordlessWebSocket() {
    const wsRef = useRef(null);

    // WebSocket 연결
    const connect = (wsUrl, pushConnectorToken, onResult) => {
        if (!wsUrl || !pushConnectorToken) {
            console.error('WebSocket URL or token is missing');
            return;
        }

        try {
            const ws = new WebSocket(wsUrl);
            wsRef.current = ws;

            ws.onopen = () => {
                console.log('WebSocket Connected');
                const handshake = JSON.stringify({
                    type: 'hand',
                    pushConnectorToken: pushConnectorToken
                });
                ws.send(handshake);
                console.log('Handshake sent:', handshake);
            };

            ws.onmessage = async (event) => {
                console.log('WebSocket message received:', event.data);
                try {
                    const result = JSON.parse(event.data);
                    if (result.type === 'result' && onResult) {
                        onResult(result);
                    }
                } catch (err) {
                    console.error('WebSocket message parse error:', err);
                }
            };

            ws.onerror = (error) => {
                console.error('WebSocket error:', error);
            };

            ws.onclose = (event) => {
                console.log('WebSocket closed:', event.code, event.reason);
            };
        } catch (error) {
            console.error('WebSocket connection failed:', error);
        }
    };

    // WebSocket 연결 종료
    const disconnect = () => {
        if (wsRef.current) {
            wsRef.current.close();
            wsRef.current = null;
        }
    };

    // 컴포넌트 언마운트 시 자동 정리
    useEffect(() => {
        return () => {
            disconnect();
        };
    }, []);

    return {
        connect,
        disconnect,
        wsRef
    };
}
