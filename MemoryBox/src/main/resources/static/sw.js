self.addEventListener('install', function (event) {
  self.skipWaiting();
});

self.addEventListener('activate', function (event) {
  event.waitUntil(clients.claim());
});

self.addEventListener('push', function (event) {
  console.log('[SW] push event received', event);

  var payload = {
    title: 'MemoryBox',
    body: '새 알림이 도착했습니다.',
    url: '/mypage'
  };

  try {
    if (event.data) {
      var data = event.data.json();
      payload = {
        title: data.title || payload.title,
        body: data.body || payload.body,
        url: data.url || payload.url
      };
      console.log('[SW] push payload parsed', payload);
    } else {
      console.log('[SW] push payload missing, using default payload');
    }
  } catch (e) {
    console.warn('[SW] push payload parse failed, using default payload', e);
  }

  console.log('[SW] showNotification about to be called', payload);
  event.waitUntil(
    self.registration.showNotification(payload.title, {
      body: payload.body,
      data: {
        url: payload.url
      }
    })
  );
});

self.addEventListener('notificationclick', function (event) {
  event.notification.close();

  var targetUrl = (event.notification.data && event.notification.data.url) || '/mypage';

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(function (windowClients) {
      for (const client of windowClients) {
        if ('navigate' in client) {
          return client.navigate(targetUrl).then(function () {
            return client.focus();
          });
        }
      }
      return clients.openWindow(targetUrl);
    })
  );
});
