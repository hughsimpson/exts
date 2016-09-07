package com.sksamuel.exts.jdbc

import java.sql.{Connection, DriverManager}

import scala.util.control.NonFatal

trait JdbcSupport {
  outer =>

  def withConnection[T](url: String,
                        autoCommit: Boolean = true,
                        schema: String = null,
                        catalog: String = null)(fn: Connection => T): T = {
    val conn = openConnection(url, autoCommit, schema, catalog)
    try {
      fn(conn)
    } finally {
      conn.close()
    }
  }

  def openConnection(url: String,
                     autoCommit: Boolean = true,
                     schema: String = null,
                     catalog: String = null): Connection = {
    val conn = DriverManager.getConnection(url)
    conn.setAutoCommit(autoCommit)
    if (schema != null)
      conn.setSchema(schema)
    if (catalog != null)
      conn.setCatalog(catalog)
    conn
  }

  def withCommit[T](conn: Connection)(f: Connection => T): T = {
    try {
      val result = f(conn)
      if (!conn.getAutoCommit)
        conn.commit()
      result
    } catch {
      case NonFatal(e) =>
        if (!conn.getAutoCommit)
          conn.rollback()
        throw e
    }
  }

  implicit class RichConnection(conn: Connection) {
    def withCommit[T](f: Connection => T): T = outer.withCommit(conn)(f)
  }
}
