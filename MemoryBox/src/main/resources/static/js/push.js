(function () {
  const toggle = document.getElementById('webPushToggle');
  const testBtn = document.getElementById('sendWebPushTestBtn');
  const statusBox = document.getElementById('webPushStatusMsg');

  if (!toggle) {
    return;
  }

  function setStatus(msg, isError) {
    if (!statusBox) {
      return;
    }
    statusBox.textContent = msg;
    statusBox.classList.toggle('is-error', !!isError);
    statusBox.classList.toggle('is-success', !isError);
  }

  function urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const rawData = atob(base64);
    const outputArray = new Uint8Array(rawData.length);

    for (let i = 0; i < rawData.length; ++i) {
      outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
  }

  async function fetchVapidPublicKey() {
    const response = await fetch('/api/push/public-key');
    if (!response.ok) {
      throw new Error('VAPID 공개키 조회 실패');
    }
    const json = await response.json();
    if (!json.publicKey) {
      throw new Error('VAPID 공개키가 비어 있습니다.');
    }
    return json.publicKey;
  }

  async function getRegistration() {
    const registration = await navigator.serviceWorker.register('/sw.js');
    await registration.update();
    return navigator.serviceWorker.ready;
  }

  async function enablePush() {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
      setStatus('이 브라우저는 Web Push를 지원하지 않습니다.', true);
      toggle.checked = false;
      return;
    }

    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
      setStatus('알림 권한이 허용되지 않았습니다.', true);
      toggle.checked = false;
      return;
    }

    const vapidPublicKey = await fetchVapidPublicKey();
    const registration = await getRegistration();

    let subscription = await registration.pushManager.getSubscription();
    if (!subscription) {
      subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(vapidPublicKey)
      });
    }

    const response = await fetch('/push/subscribe', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(subscription)
    });

    if (!response.ok) {
      throw new Error('구독 저장 요청 실패');
    }

    toggle.checked = true;
    setStatus('웹 푸시 알림이 활성화되었습니다.', false);
  }

  async function disablePush() {
    const registration = await getRegistration();
    const subscription = await registration.pushManager.getSubscription();

    if (subscription) {
      await fetch('/push/unsubscribe', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ endpoint: subscription.endpoint })
      });
      await subscription.unsubscribe();
    }

    toggle.checked = false;
    setStatus('웹 푸시 알림이 비활성화되었습니다.', false);
  }

  async function syncToggleState() {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
      toggle.checked = false;
      toggle.disabled = true;
      setStatus('이 브라우저는 Web Push를 지원하지 않습니다.', true);
      return;
    }

    if (Notification.permission === 'denied') {
      toggle.checked = false;
      setStatus('브라우저 설정에서 알림이 차단되어 있습니다.', true);
      return;
    }

    const registration = await getRegistration();
    const subscription = await registration.pushManager.getSubscription();
    toggle.checked = !!subscription;
  }

  toggle.addEventListener('change', async function () {
    toggle.disabled = true;
    try {
      if (toggle.checked) {
        await enablePush();
      } else {
        await disablePush();
      }
    } catch (error) {
      toggle.checked = !toggle.checked;
      setStatus('웹 푸시 설정 변경 중 오류가 발생했습니다.', true);
    } finally {
      toggle.disabled = false;
    }
  });

  if (testBtn) {
    testBtn.addEventListener('click', async function () {
      testBtn.disabled = true;
      try {
        const response = await fetch('/push/test', { method: 'POST' });
        if (!response.ok) {
          throw new Error('테스트 발송 실패');
        }
        const json = await response.json();
        setStatus('테스트 푸시 발송 완료 (성공 ' + json.sent + ' / 전체 ' + json.total + ')', false);
      } catch (error) {
        setStatus('테스트 푸시 발송 중 오류가 발생했습니다.', true);
      } finally {
        testBtn.disabled = false;
      }
    });
  }

  syncToggleState().catch(function () {
    setStatus('웹 푸시 상태를 불러오지 못했습니다.', true);
  });
})();
