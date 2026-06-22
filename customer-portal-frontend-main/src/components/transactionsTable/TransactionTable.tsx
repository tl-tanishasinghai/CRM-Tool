'use client'

import React, { useState } from "react";
import "./TransactionTable.scss";
import Modal from "@/components/modal/Modal";
import Table from "@/components/table/Table";

type TransactionRow = {
  date: string;
  amount: string | number;
  status?: string;
};

const columns = [
  { label: "Date", key: "date" },
  { label: "Status", key: "status", defaultValue: "Completed" },
  { label: "Amount", key: "amount" },
] as const;

interface TransactionTableProps {
  transactionsList: TransactionRow[];
}

export default function TransactionTable({ transactionsList }: TransactionTableProps) {
  const [showAllTransactions, setShowAllTransactions] = useState(false);

  return (
    <>
      <div className="widget-header">
        <div className="widget-heading">Transactions</div>
        {transactionsList.length !== 0 && (
          <button
            className="widget-actionable transaction-button"
            onClick={() => setShowAllTransactions(true)}
            type="button"
          >
            View Last {transactionsList.length} transactions
          </button>
        )}
      </div>
      <Table transactionsList={transactionsList.slice(0, 1)} columns={columns} />
      <Modal isOpen={showAllTransactions} onClose={() => setShowAllTransactions(false)}>
        <div className="modal-table-container">
          <div className="modal-table-header">
            <div className="widget-heading">
              Last {transactionsList.length} transactions
            </div>
            <div
              className="transaction-button"
              onClick={() => setShowAllTransactions(false)}
            >
              ✕
            </div>
          </div>
          <Table transactionsList={transactionsList} columns={columns} />
        </div>
      </Modal>
    </>
  );
}

// const Table = (props) => {
//   const { transactionsList } = props;
//   return (
//     <div className="table-container">
//       {transactionsList.length ? <table className="transaction-table">
//         <thead>
//           <tr>
//             <th>Date</th>
//             {/* <th>Time</th> */}
//             <th>Status</th>
//             <th>Amount</th>
//           </tr>
//         </thead>
//         <tbody>
//           {transactionsList?.length ==0 ? <div className="no-transactions">No EMI found</div>: transactionsList.map((tx, index) => (
//             <tr
//               key={index}
//               // className={index === transactionsList.length - 1 ? "last-row" : ""}
//             >
//               <td>{tx.date}</td>
//               {/* <td>{tx.time}</td> */}
//               {/* <td>{tx.value}</td> */}
//               <td>Completed</td>
//               <td>{tx.amount}</td>
//             </tr>
//           ))}
//         </tbody>
//       </table> : <div className="no-data-found">No Transactions found</div>}
//     </div>
//   );
// };
