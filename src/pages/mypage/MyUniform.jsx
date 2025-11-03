import React, { useState } from 'react';
import StatusBar from '/components/StatusBar';
import TeamSelect from '/components/TeamSelect';
import Loading from '/components/Loading';
import { useAuth } from "/context/AuthContext";
import { useInit } from "/context/InitContext";
import { generateImage, getMyImages, saveAiUniformImage } from '/services/my.js';

const MyUniform = () => {
  const { auth } = useAuth();
  const { teams } = useInit();
  const [selectedTeam, setSelectedTeam] = useState({});
  const [prompt, setPrompt] = useState('');
  const [size, setSize] = useState('1024x1024');
  const [saveToFile, setSaveToFile] = useState(true);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [savedImages, setSavedImages] = useState([]);
  const [activeTab, setActiveTab] = useState('single');
  
  // 이미지 생성
  const generateImages = async () => {
    if (!prompt.trim()) {
      setError('프롬프트를 입력해주세요.');
      return;
    }

    if (!selectedTeam.id) {
      setError('팀을 선택해주세요.');
      return;
    }

    if (!auth?.id) {
      setError('로그인이 필요합니다.');
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      // 1단계: 이미지 생성 (FastAPI에서 Base64 변환까지 완료)
      const data = await generateImage({
        korean_prompt: prompt,
        size: size,
        save_to_file: saveToFile,
        user_id: auth.id,
        team_name: selectedTeam.name  // 팀 이름 전달
      });
      
      if (data.success && data.image_base64) {
        // 2단계: DB에 저장 (Spring Boot)
        const saveData = {
          userId: auth.id,
          teamId: null,  // 유니폼 이미지는 팀 정보 없이 사용자에게만 연결
          koreanPrompt: data.korean_prompt,
          englishPrompt: data.english_prompt,
          imageBase64: data.image_base64,
          imageUrl: data.image_url,
          filename: data.filename || `ai_uniform_${Date.now()}.png`,
          fileSize: data.file_size,
          imageSize: size
        };
        
        await saveAiUniformImage(saveData);
        
        setResult(data);
        
        // 갤러리 새로고침
        setTimeout(() => {
          loadSavedImages();
        }, 500);
      } else {
        setError(data.message || '이미지 생성에 실패했습니다.');
      }
    } catch (err) {
      console.error('이미지 생성 에러:', err);
      console.error('에러 응답 데이터:', err.response?.data);
      const errorMsg = err.response?.data?.message || err.message || '서버 연결 오류';
      setError('서버 오류: ' + errorMsg);
    } finally {
      setLoading(false);
    }
  };


  // 저장된 이미지 목록 로드
  const loadSavedImages = async () => {
    if (!auth?.id) {
      return;
    }
    
    try {
      const data = await getMyImages(auth.id);
      setSavedImages(data || []);
    } catch (err) {
      console.error('이미지 목록 로드 실패:', err);
      setSavedImages([]);
    }
  };

  // 컴포넌트 마운트 시 이미지 목록 로드
  React.useEffect(() => {
    if (auth?.id) {
      loadSavedImages();
    }
  }, [auth?.id]);

  // 갤러리 탭 활성화 시 이미지 목록 새로고침
  React.useEffect(() => {
    if (activeTab === 'gallery' && auth?.id) {
      loadSavedImages();
    }
  }, [activeTab, auth?.id]);

  const tabStyle = (isActive) => ({
    color: isActive ? 'var(--point-color)' : 'var(--text-dim-color)',
    borderBottom: isActive ? '3px solid var(--point-color)' : 'none',
    fontWeight: isActive ? '600' : '400',
    backgroundColor: isActive ? '#fff' : 'transparent'
  });

  const inputStyle = {
    borderColor: 'var(--gray-border-color)',
    borderRadius: '8px',
    padding: '12px',
    fontSize: '14px',
    color: 'var(--text-color)',
    backgroundColor: '#fff'
  };

  const labelStyle = { color: 'var(--text-color)', fontWeight: '600' };

  const buttonStyle = (isDisabled) => ({
    backgroundColor: isDisabled ? 'var(--gray-border-color)' : 'var(--point-color)',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    padding: '12px 24px',
    fontSize: '16px',
    fontWeight: '600',
    width: '100%',
    cursor: isDisabled ? 'not-allowed' : 'pointer',
    transition: 'all 0.3s ease'
  });

  return (
    <>
      <StatusBar title="팀 유니폼 제작" />
      <section className="d-flex flex-column overflow-y-auto" style={{ marginTop: 0, minHeight: "100%", height: '100%', background: '#fff' }}>
        <div className="container p-4">
          <label className="h3 mb-4">⚾ AI 유니폼 생성기</label>
        
        {/* 탭 메뉴 */}
        <ul className="nav nav-tabs mb-4 gap-8" style={{ borderBottom: '2px solid var(--gray-border-color)' }}>
          <li className="nav-item">
            <button 
              className={`nav-link ${activeTab === 'single' ? 'active' : ''}`}
              onClick={() => setActiveTab('single')}
              style={tabStyle(activeTab === 'single')}
            >
              이미지 생성
            </button>
          </li>
          <li className="nav-item">
            <button 
              className={`nav-link ${activeTab === 'gallery' ? 'active' : ''}`}
              onClick={() => setActiveTab('gallery')}
              style={tabStyle(activeTab === 'gallery')}
            >
              갤러리
            </button>
          </li>
        </ul>

        {/* 이미지 생성 */}
        {activeTab === 'single' && (
          <div className="tab-content">
            {/* 팀 선택 */}
            <div className="mb-3">
              <label className="form-label mb-2" style={labelStyle}>팀 선택</label>
              <div className="d-flex p-0 flex-wrap" style={{ zoom: 0.85 }}>
                {teams.map((team) => (
                  <div key={team.id}>
                    <div style={{ transform: 'scale(0.84)', width: '100%', height: '160px' }}>
                      <TeamSelect team={team} selected={selectedTeam} setTeam={setSelectedTeam} />
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="mb-3">
              <label className="form-label" style={labelStyle}>프롬프트</label>
              <textarea
                className="form-control"
                value={prompt}
                onChange={(e) => setPrompt(e.target.value)}
                placeholder={selectedTeam.name 
                  ? `예: ${selectedTeam.name} 나만의 유니폼 생성해줘` 
                  : "팀을 먼저 선택해주세요"}
                rows={3}
                style={inputStyle}
                disabled={!selectedTeam.id || loading}
              />
            </div>

            <button 
              onClick={generateImages} 
              disabled={loading || !selectedTeam.id}
              className="btn btn-primary"
              style={buttonStyle(loading || !selectedTeam.id)}
            >
              {loading ? '생성 중...' : '유니폼 생성'}
            </button>

            {loading && (
              <div className="text-center mt-5">
                <Loading />
                <p className="mt-3" style={{ color: 'var(--text-dim-color)' }}>
                  유니폼을 생성하고 있습니다. 잠시만 기다려주세요...
                </p>
              </div>
            )}

            {/* 에러 메시지 */}
            {error && activeTab === 'single' && (
              <div 
                className="alert alert-danger mt-3" 
                role="alert"
                style={{
                  backgroundColor: '#f8d7da',
                  color: '#721c24',
                  border: '1px solid #f5c6cb',
                  borderRadius: '8px',
                  padding: '12px 16px',
                  marginTop: '16px'
                }}
              >
                {error}
                <button 
                  type="button" 
                  className="btn-close" 
                  onClick={() => setError(null)}
                  style={{ float: 'right', border: 'none', background: 'none', fontSize: '18px', cursor: 'pointer' }}
                >
                  ×
                </button>
              </div>
            )}

            {/* 결과 표시 */}
            {result && activeTab === 'single' && (
              <div className="mt-4">
                {result.success ? (
                  <div 
                    className="alert alert-success"
                    style={{
                      backgroundColor: '#d4edda',
                      color: '#155724',
                      border: '1px solid #c3e6cb',
                      borderRadius: '8px',
                      padding: '16px'
                    }}
                  >
                    <label className="mb-3" style={{ fontWeight: '600', fontSize: '16px' }}>✅ 유니폼이 성공적으로 생성되었습니다!</label>
                    {result.image_url && (
                      <div className="text-center my-3">
                        <img 
                          src={result.image_url} 
                          alt="Generated Uniform" 
                          className="img-fluid" 
                          style={{
                            maxHeight: '500px', 
                            borderRadius: '12px', 
                            boxShadow: '0 6px 16px rgba(0,0,0,0.15)'
                          }} 
                        />
                      </div>
                    )}
                    <div className="text-center mt-3">
                      <p className="small text-muted mb-0">
                        생성된 이미지는 자동으로 갤러리에 저장됩니다.
                      </p>
                    </div>
                  </div>
                ) : (
                  <div 
                    className="alert alert-danger"
                    style={{
                      backgroundColor: '#f8d7da',
                      color: '#721c24',
                      border: '1px solid #f5c6cb',
                      borderRadius: '8px',
                      padding: '16px'
                    }}
                  >
                    <label style={{ color: '#dc3545', fontWeight: '600' }}>생성 실패</label>
                    <p style={{ color: 'var(--text-color)' }}>{result.message}</p>
                  </div>
                )}
              </div>
            )}
          </div>
        )}


        {/* 갤러리 */}
        {activeTab === 'gallery' && (
          <div className="tab-content">
            <div className="mb-3">
              <label className="mb-0" style={labelStyle}>저장된 이미지</label>
            </div>
            <div className="row">
              {savedImages.map((image, index) => (
                <div key={index} className="col-md-6 col-lg-4 mb-3">
                  <div 
                    className="card"
                    style={{
                      border: '1px solid var(--gray-border-color)',
                      borderRadius: '12px',
                      overflow: 'hidden',
                      boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
                      transition: 'transform 0.2s ease'
                    }}
                  >
                    <img 
                      src={image.imageBase64 ? `data:image/png;base64,${image.imageBase64}` : '/api/ai/download/default.png'}
                      alt={image.koreanPrompt || 'AI 유니폼'}
                      className="card-img-top"
                      style={{
                        height: '200px', 
                        objectFit: 'cover',
                        borderBottom: '1px solid var(--gray-border-color)'
                      }}
                      onError={(e) => {
                        e.target.style.display = 'none';
                      }}
                    />
                    <div className="card-body" style={{ padding: '12px' }}>
                      <h6 
                        className="card-title text-truncate" 
                        style={{ 
                          color: 'var(--text-color)', 
                          fontSize: '14px',
                          fontWeight: '600',
                          marginBottom: '8px'
                        }}
                      >
                        {image.koreanPrompt || image.filename}
                      </h6>
                      <p 
                        className="card-text small"
                        style={{ 
                          color: 'var(--text-dim-color)', 
                          fontSize: '12px',
                          marginBottom: '12px'
                        }}
                      >
                        크기: {image.fileSize ? (image.fileSize / 1024).toFixed(1) + ' KB' : 'N/A'}<br/>
                        생성: {image.createdAt ? new Date(image.createdAt).toLocaleString() : 'N/A'}
                      </p>
                      <a 
                        href={image.imageBase64 ? `data:image/png;base64,${image.imageBase64}` : '#'}
                        download={image.filename || 'ai_uniform.png'}
                        className="btn btn-sm"
                        style={{
                          backgroundColor: 'var(--point-color)',
                          color: '#fff',
                          border: 'none',
                          borderRadius: '6px',
                          padding: '6px 12px',
                          fontSize: '12px',
                          fontWeight: '500',
                          textDecoration: 'none',
                          display: 'inline-block'
                        }}
                      >
                        다운로드
                      </a>
                    </div>
                  </div>
                </div>
              ))}
              {savedImages.length === 0 && (
                <div className="col-12 text-center py-5">
                  <p 
                    style={{ 
                      color: 'var(--text-dim-color)', 
                      fontSize: '16px',
                      fontStyle: 'italic'
                    }}
                  >
                    저장된 이미지가 없습니다.
                  </p>
                </div>
              )}
            </div>
          </div>
        )}
        </div>
      </section>
    </>
  );
};

export default MyUniform;
