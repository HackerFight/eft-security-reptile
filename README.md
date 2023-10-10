## 1. 请求头增加参数
1. 请求头header中携带一个参数，key=by-security-policy-key， value 是每次请求随机生成的一个64位字符串
2. 前端需要对参数值进行加密后再放到header中发送给后端，后端用私钥解密验证
3. 解密后验证前端传入的参数值是否符合特定规则，约定规则如下：
   1.要求字符串长度一共是64个字符，必须等于64，其中包含字符和数字，且字符都是小写字符，
   2.如果最后一位是数字，那么第7,14,21,28位置的字符要求必须是数字3
   3.如果最后一位是字符，则要求第7,14,21,28位置的元素为6,7,8,9这四个数字中的任意一个，
   4.除了特定位置上必须是数字外，其他位置的元素不做要求，可以是数字也可以是字符。
4. 样例：dSAaAuS/7t4MiMIjJng+36Qni5s9pKPo5gO7cSbBK3fZCf+ljAZ8IHs/juuGg/IsBHNlQbxRAJIQt1suOXaVl+H/ygzOaHaNBkuqScqPRODSn8hLZVO/nc++j1q+vt4+p9ItM+nu8FCIxMykaoOKdMvPdFv5acv86SFt/FJDr8cLkAsrD9iGkMnaMeTT/yMr86AOt52aWGVDUzWO0Hw8Uw+clldWKCNwJD7RJ/CDrUv2OjlDV9E8LkT6zV05D2UwGOC0TZmYL7IoaLql1eRnBy13xNz01Mkoa2hByqX5tI+NN7l5lyBEOOdVsjy65ag8qpkd6nyXRi1vTugL6mefZg==

## 2. 请求头增加行为收集参数
   参数的key=behavior-list， 这个参数主要是用来记录用户是否有类人行为，比如是否点击了鼠标，是否有输入 库聚焦的工作，如果没有，则认为是机器人。

## 3. redis 配置
   这个要求后端集成redis, 用来做防刷和限制请求的

## 4. 发送邮件
   1. 可以配置是否开启发送邮件，如果开启，则配置发送邮件的主机，发送的目标地址，以及标题等其他信息。具体配置如下：
```yaml
eft:
  security:
    reptile:
      mailConfig:
        enable: true  --是否开启，默认是false
        host: mail.eft.cn    -- 发送邮件的主机
        username: saas_helper@efreight.com.cn  --发送邮件的用户名
        password: saas_helper.eFt20190712   -- 发送邮件的密码
        nickname: '翌飞小云'
        port: 465
        title: 爬虫告警   -- 发送邮件的标题
        to:    -- 发送的目标机器
          - mayt@efreight.cn
          - wuting@efreight.cn
          - fuyunhui@efreight.cn
          - 1281955045@qq.com
        cc:   -- 抄送的目标机器
          - fuyuanhui@efreight.cn
        useSSL: true
```

 2. 用户需要指定发送邮件的内容，可以实现 `MailTemplatePostProcessor` 接口