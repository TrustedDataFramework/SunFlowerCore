const froms =
    [
        '02b507fe1afd0cc7a525488292beadbe9f143784de44f8bc1c991636509fd50936',
        '02f9d915954e04107d11fb9689a6330c22199e1e830857bff076e033bbca2888d4'
    ];

const crypto = require('crypto');

const entry = 'http://localhost:30501';
const count = 800;
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

setInterval(transfer, 9000);

