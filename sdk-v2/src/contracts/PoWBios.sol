// SPDX-License-Identifier: GPL-3.0

pragma solidity >=0.7.0 <0.9.0;

interface PowBios {
    // called when coinbase transaction executed
    function update() external;

    // get current nbits
    function nbits() external view returns (uint256);
}