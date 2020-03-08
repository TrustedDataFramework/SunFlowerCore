const from =
    '02b507fe1afd0cc7a525488292beadbe9f143784de44f8bc1c991636509fd50936';

const crypto = require('crypto');

const entry = 'http://localhost:7080';
const count = 500;
const axios = require('axios');

const transaction = {
    version: 1634693120,
    type: 1,
    from: from,
    signature: 'ff',
    createdAt: Math.floor(Date.now() / 1000),
    nonce: 2,
    gasPrice: 1,
    amount: 1,
    to: crypto.randomBytes(20).toString('hex')
};


const transfer = () => axios
    .get(`${entry}/rpc/account/${from}`)
    .then(resp => resp.data)
    .then(data => {
        const body = [];
        transaction.nonce = data.data.nonce + 1;
        for (let i = 0; i < count; i++) {
            body.push(JSON.parse(JSON.stringify(transaction)));
            transaction.nonce++;
            transaction.to = crypto.randomBytes(20).toString('hex');
        }
        return axios.post(`${entry}/rpc/transaction`, body);
    })
    .then(console.log)
    .catch(console.error);

setInterval(transfer, 6000);

