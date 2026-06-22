'use client'

import React from "react";
import "./Table.scss";

type TableColumn<T> = {
  key: keyof T;
  label: string;
  render?: (value: T[keyof T], row: T) => React.ReactNode;
  defaultValue?: React.ReactNode;
};

type TableProps<T> = {
  transactionsList: T[];
  columns: ReadonlyArray<TableColumn<T>>;
};

const Table = <T extends Record<string, React.ReactNode>>({
  transactionsList,
  columns,
}: TableProps<T>) => {
  return (
    <div className="table-container">
      {transactionsList.length > 0 ? (
        <table className="transaction-table">
          <thead>
            <tr>
              {columns.map((col) => (
                <th key={String(col.key)}>{col.label}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {transactionsList.map((tx, rowIndex) => (
              <tr key={rowIndex}>
                {columns.map((col) => {
                  const value = tx[col.key];
                  return (
                    <td key={String(col.key)}>
                      {col.render
                        ? col.render(value, tx)
                        : value ?? col.defaultValue ?? "-"}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <div className="no-data-found">No Transactions found</div>
      )}
    </div>
  );
};

export default Table;
