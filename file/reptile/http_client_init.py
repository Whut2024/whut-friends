import json

import httpx


class Http2Client:
    def __init__(self):
        self.headers = {
            'cookie': 'SESSION=OTEwM2MzNDQtMGNlMi00YzE4LTlmODAtNTJkZDM2ZGNjZGM5; Max-Age=2592000; Expires=Thu, 3 Oct 2024 11:27:39 GMT; Domain=mianshiya.com; Path=/api; HttpOnly; SameSite=Lax`',
            'user-agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36 MicroMessenger/6.8.0(0x16080000) NetType/WIFI MiniProgramEnv/Mac MacWechat/WMPF MacWechat/3.8.8(0x13080813) XWEB/1227',
            'content-type': 'application/json',
            'xweb_xhr': '1',
        }

        self.client = httpx.Client(http2=True)

    def get_request(self, url):
        return self.client.get(url=url, headers=self.headers)

    def post_request(self, url, data):
        return self.client.post(url=url, json=data, headers=self.headers)
