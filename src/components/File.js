export function fileToBase64(file) {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    return new Promise((resolve, reject) => {
        reader.onload = () => resolve(reader.result);
        reader.onerror = () => reject(reader.error);
    });
}
//동작이 이행이 되면 resolve 함수를, 에러가 발생하면 reject 동작을 수행 한다.

export function base64ToFile(str) {
    return new File(
        [Uint8Array.from(btoa(str), (m) => m.codePointAt(0))],
        'ticket.png',
        { type: str.split(/data:|;/gi)[1] }
    );
}

export default {};
