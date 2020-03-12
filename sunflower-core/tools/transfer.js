const froms =
    [
        '02b5e348618a86fcd42ced0d6b64737f4a3674237ad83c39fd968db0f59a46fe88',
        '02edf699137453c6139f6d1a05f9016021e0d85d89c3a97ba454ed0c8a1bfbb3ab',
        '03079b102888c9d61784a8b31c4fa5f17132ebcf5fcb4f1ddad2534a8b33317205',
        '038e8204502bc18df6d5d719097a64e0991162780e22c83fa9ddb6c791c758cb6e',
        '02b92fd1932454b5e8ee3b3adecb3404bc167317915ef1192eb431f5c37cb3d787',
        '03eb5677f04c1a963bbd48d923af17294307bad765118a7241fc7247f16ab04170',
        '02f9da05ba103f4e8d63b9baf2cb01f61d2f43e73e836bf91486117ed02b7eddda'
    ];

// 38842c0c2c4caadaf88af398777f274637c3dbce
// f5cdfdff8e0ed4dd72474862f619c5ca54de4364
//  75f58973b527b60ece47d4b40a92ca6ec18807ff
//  8679a9a873351a49246bd3166cd7d0d17744118f
//  3d6d54686ce9af55323e96b70f09c35220a37a00
//  daf32f9e4da2eec6fd05e7c10b45575e20e81406
//  bae996c55f3e6e00a8a348a349608488f509b927
const crypto = require('crypto');

const entry = 'http://192.168.1.118:8085';
// const entry = 'http://localhost:8888';
const count = 150;
const axios = require('axios');

const transaction = {
    version: 1634693120,
    type: 1,
    from: '',
    signature: 'ff',
    createdAt: Math.floor(Date.now() / 1000),
    nonce: 2,
    gasPrice: 1,
    amount: 1,
    to: crypto.randomBytes(20).toString('hex')
};


const transfer = () => froms.forEach(from => {
    axios
        .get(`${entry}/rpc/account/${from}`)
        .then(resp => resp.data)
        .then(data => {
            const body = [];
            const nonce = data.data.nonce + 1;
                for (let i = 0; i < count; i++) {
                    const t = JSON.parse(JSON.stringify(transaction));
                    t.from = from;
                    t.nonce = nonce + i;
                    t.to = crypto.randomBytes(20).toString('hex');
                    body.push(t);
                }
            return axios.post(`${entry}/rpc/transaction`, body);
        })
        .then(() => console.log('success'))
        .catch(console.error);
})

setInterval(transfer, 7000);

