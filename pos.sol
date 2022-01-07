// SPDX-License-Identifier: GPL-3.0

pragma solidity ^0.8.0;

interface IPoS {
    struct Vote {
        address from;
        address to;
        uint256 amount;
        uint256 number;
    }

    function __coinbase__(address payable miner) external payable;

    // consensus constants
    function defaultMiners() external view returns (address[] memory);
    function eraSize() external view returns (uint256);
    function blockInterval() external view returns(uint256);
    function proposerMinStake() external view returns(uint256);
    function maxMiners() external view returns(uint256);
    function declineEras() external view returns(uint256);
    function initialReward() external view returns(uint256);


    // staking/unstaking
    function balanceOf(address _owner) external view returns(uint256);
    function lastMinedAt(address miner) external view returns(uint256);
    function deposit(address benefit) external payable;
    // miner cannot withdraw
    function withdraw(address payable benefit) external payable;

    // score/vote
    function scoreOf(address _owner) external view returns(uint256);

    // number of candidates
    function n() external view returns (uint256);

    // vote to somebody
    function vote(address benefit, address candidate) external payable;

    // vote to somebody at index i
    function vote(uint256 i, address benefit, address candidate) external payable;

    // cancel vote
    function cancelVote(uint256 i, address payable benefit) external payable;

    // vote at index i
    function votes(uint256 i) external view returns(address from, address to, uint256 amount, uint256 number, uint256 score);

    function candidates(uint256 i) external view returns(address);
    function candidateIndex(address a) external view returns(uint256);


    // pow target
    function target() external view returns (uint256);
    function getProposer(address parentMiner, uint256 parentCreatedAt, address[] calldata candidates_, uint256 timestamp) external pure returns (address, uint256, uint256) ;

}

contract PoS is IPoS {
    function defaultMiners() external override pure returns (address[] memory) {
        address[] memory r = new address[](3);
        r[0] = 0xcCB8f00D1b4B37Cc9a36ee5956cE07371B1b72C7;
        r[1] = 0xEb89161Bd860E218042dF5e8Aa4fB7F445a46c16;
        r[2] = 0x3045459EBDa695fDC5237478dd31242CF0307498;
        return r;
    }

    uint256 public constant override initialReward = 400000 * 1e18;
    uint256 public constant override eraSize = 120;
    uint256 public constant override blockInterval = 3;
    uint256 public constant override proposerMinStake = 100000 * 1e18;
    uint256 public constant override declineEras = 24 * 60 * 60 * 30 / (eraSize * blockInterval) ;
    uint256 public constant override maxMiners = 5;
    uint256 public constant override target = 2315841784746323908471419700173758157065399693312811280789151680158262592 * 64;

    mapping(address => uint256) public override balanceOf;

    mapping(address => uint256) public override lastMinedAt;

    // candiate => votes
    mapping(uint256 => Vote) private votes_;

    // candidates
    mapping(uint256 => address) public override candidates;

    // index of candidate
    mapping(address => uint256) public override candidateIndex;

    // global candidates counter;
    uint256 public override n;


    // previous timestamps
    mapping(uint256 => uint256) public timestamps;


    uint256 public timestampsN = 0;


    function deposit(address benefit) external payable override {
        require(msg.value > 0);
        balanceOf[benefit] += msg.value;

        if(balanceOf[benefit] < proposerMinStake)
            return;

        if(candidateIndex[benefit] == 0) {
            n++;
            candidateIndex[benefit] = n;
            candidates[n] = benefit;
        }

    }

    function withdraw(address payable benefit) external payable override {
        require(balanceOf[msg.sender] > 0);
        require(block.number - lastMinedAt[msg.sender] > eraSize);
        uint256 amount = balanceOf[msg.sender];
        balanceOf[msg.sender] = 0;
        benefit.transfer(amount);
    }

    function __coinbase__(address payable miner) external override payable {
        require(msg.sender == address(0));

        lastMinedAt[miner] = block.number;

        miner.transfer(msg.value);

        if(address(this).balance == 0)
            return;

        uint256 y = block.number * blockInterval / 60 / 60 / 24 / 356;
        uint256 reward = initialReward;

        for(uint256 i = 0; i < y; i++)
            reward = reward * 80 / 100;

        reward = reward < address(this).balance ? reward : address(this).balance;
        // transfer bonus to receiver
        miner.transfer(reward);
    }


    function _updateTimestamp() internal {
        timestampsN++;

        timestamps[timestampsN - 1] = block.timestamp;

        if(timestampsN != eraSize) {
            return;
        }

        uint256 duration = timestamps[eraSize - 1] - timestamps[0];

        uint256 adjusted;
    unchecked { adjusted = target * duration; }
        if(adjusted < target)
            adjusted = target;

        adjusted = adjusted / ((eraSize - 1) * blockInterval);
        uint256 max;

    unchecked { max = target * 16; }
        if(max < target)
            max = target;

        uint256 min = target / 16;

        if(adjusted < min) {
        } else if(adjusted > max) {
        } else {
        }

        // clear the array
        timestampsN = 0;
    }

    function scoreOf(address _owner) external override view returns(uint256) {
        uint256 i = 0;
        uint256 s = 0;
        while(true) {
            (address from, address to, , , uint256 score)  = votes(i);

            if(from == address(0))
                break;

            if(to == _owner)
                s += score;
            i++;
        }
        return s;
    }


    function votes(uint256 i) public view override returns(address from, address to, uint256 amount, uint256 number, uint256 score) {
        from = votes_[i].from;
        to = votes_[i].to;
        amount = votes_[i].amount;
        number = votes_[i].number;
        uint256 declines = (block.number - number) / eraSize / declineEras;

        score = amount;
        while(declines > 0) {
            score = score * 9 / 10;
            declines--;
        }
    }

    function _vote(uint256 i, address voter, address benefit, uint256 amount) internal {
        votes_[i].from = voter;
        votes_[i].to = benefit;
        votes_[i].amount = amount;
        votes_[i].number = block.number;
    }


    // vote to somebody
    function vote(address benefit, address candidate) external override payable {
        require(msg.value > 0);
        require(balanceOf[candidate] >= proposerMinStake);
        uint256 i = 0;
        while(votes_[i].amount != 0)
            i++;
        _vote(i, benefit, candidate, msg.value);
    }

    // vote to somebody
    function vote(uint256 i, address benefit, address candidate) external override payable {
        require(msg.value > 0);
        require(balanceOf[candidate] >= proposerMinStake);
        require(i == 0 || votes_[i - 1].amount != 0);
        require(votes_[i].amount == 0);
        _vote(i, benefit, candidate, msg.value);
    }

    // withdraw
    function cancelVote(uint256 i, address payable benefit) external payable override {
        require(votes_[i].amount > 0 && votes_[i].from == msg.sender);
        benefit.transfer(votes_[i].amount);
        votes_[i].amount = 0;
    }


    // get proposer
    function getProposer(address parentMiner, uint256 parentCreatedAt, address[] calldata candidates_, uint256 timestamp) external override pure returns (address, uint256, uint256) {
        require(candidates_.length > 0);
        if(timestamp <= parentCreatedAt + blockInterval)
            return (address(0), 0, 0);

        uint256 i = candidates_.length - 1;
        for(uint256 j = 0; j < candidates_.length; j++) {
            if(candidates_[j] == parentMiner) {
                i = j;
                break;
            }
        }

        uint256 step = (timestamp - parentCreatedAt) / blockInterval;
        uint256 cur = (i + step) % candidates_.length;
        uint256 start = parentCreatedAt + step * blockInterval;
        uint256 end = start + blockInterval;
        return (candidates_[cur], start, end);
    }
}