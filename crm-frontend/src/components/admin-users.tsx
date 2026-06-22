'use client'

import { FormEvent, useEffect, useState } from 'react'
import { createUser, fetchUsers } from '@/lib/api'
import type { StaffUser } from '@/lib/types'

export function AdminUsers() {
  const [users, setUsers] = useState<StaffUser[]>([])
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [role, setRole] = useState('AGENT')
  const [leadId, setLeadId] = useState('lead-1')

  async function refresh() {
    setUsers(await fetchUsers())
  }

  useEffect(() => {
    refresh().catch(() => undefined)
  }, [])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await createUser({
      name,
      email,
      role,
      leadId: role === 'AGENT' ? leadId : undefined,
      status: 'ACTIVE'
    })
    setName('')
    setEmail('')
    await refresh()
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <p className="eyebrow">Admin</p>
          <h2>Account creation and role management</h2>
        </div>
      </div>

      <div className="grid two">
        <section className="panel">
          <h3>Create user</h3>
          <form className="grid" onSubmit={handleSubmit}>
            <input
              placeholder="Full name"
              value={name}
              onChange={(event) => setName(event.target.value)}
              required
            />
            <input
              placeholder="Email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
            <select value={role} onChange={(event) => setRole(event.target.value)}>
              <option value="AGENT">Agent</option>
              <option value="LEAD">Lead</option>
              <option value="ADMIN">Admin</option>
            </select>
            {role === 'AGENT' && (
              <input
                placeholder="Lead manager user ID"
                value={leadId}
                onChange={(event) => setLeadId(event.target.value)}
              />
            )}
            <button>Create account</button>
          </form>
        </section>

        <section className="panel">
          <h3>Seeded access</h3>
          <p className="meta">
            MVP staff auth uses seeded/demo credentials. Every user signs in with password
            `password` until SSO or a password store is connected.
          </p>
        </section>
      </div>

      <section className="panel" style={{ marginTop: 16 }}>
        <h3>CRM users</h3>
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th>Lead manager</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>{user.name}</td>
                <td>{user.email}</td>
                <td>{user.role}</td>
                <td>{user.status}</td>
                <td>{user.leadId || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  )
}
